package com.web.campus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static com.web.campus.CampusPostDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
class CampusPostIntegrationTest extends CampusPostTestSupport {
    @Test
    void draftReviewEditAndRemovalRespectVersionsAndNeverExposeUnpublishedBodies() {
        var draft = create(author, "draft title", "private first body", false);
        long post = id(draft);
        assertEquals("DRAFT", draft.get("status"));
        assertEquals(0, list(peer, "feed", null, null, 0, 20).total());
        assertEquals(1, list(author, "mine", null, "DRAFT", 0, 20).total());
        fails(403, () -> posts.get(peer, post));
        fails(403, () -> posts.like(peer, post, true));
        fails(409, () -> posts.like(author, post, true));
        var pending = posts.update(author, post, write("submitted", "body version two", true, version(draft)));
        fails(409, () -> posts.review(manager, post, new Review("APPROVE", "", version(draft))));
        var rejected = posts.review(manager, post, new Review("REJECT", "请补充内容", version(pending)));
        assertEquals("REJECTED", rejected.get("status"));
        assertEquals("请补充内容", rejected.get("reviewReason"));
        var resubmitted = posts.update(author, post, write("revised", "body version three", true, version(rejected)));
        var published = posts.review(manager, post, new Review("APPROVE", "", version(resubmitted)));
        assertEquals("body version three", posts.get(peer, post).get("content"));
        var edited = posts.update(author, post, write("new edit", "new private body", true, version(published)));
        assertEquals("PENDING", edited.get("status"));
        fails(403, () -> posts.get(peer, post));
        assertEquals(0, list(peer, "feed", "new private body", null, 0, 20).total());
        fails(409, () -> posts.review(manager, post, new Review("APPROVE", "", version(published))));
        var approved = posts.review(manager, post, new Review("APPROVE", "", version(edited)));
        var pinned = posts.pin(manager, post, new Pin(true, version(approved)));
        assertEquals(true, pinned.get("pinned"));
        fails(409, () -> posts.update(author, post, write("stale", "must not overwrite", true, version(approved))));
        posts.delete(author, post, version(pinned));
        fails(404, () -> posts.get(author, post));
        fails(404, () -> posts.get(manager, post));
        fails(404, () -> posts.update(author, post, write("restore", "must not restore", true, version(pinned) + 1)));
        var tombstone = list(author, "mine", null, "REMOVED", 0, 20).list().get(0);
        assertEquals("", tombstone.get("title")); assertEquals("", tombstone.get("content"));
        assertEquals(List.of(), tombstone.get("images")); assertEquals(false, tombstone.get("canEdit"));
        assertEquals(0, list(author, "mine", "new private body", null, 0, 20).total());
    }

    @Test
    void administratorsCannotSelfReviewOrModerateOtherSchoolsAndAnnouncementsRequireAuthority() {
        fails(403, () -> posts.create(author, school, new PostWrite("公告", "公告内容", "ANNOUNCEMENT", List.of(), true, null)));
        var announcement = posts.create(manager, school, new PostWrite("公告", "公告内容", "ANNOUNCEMENT", List.of(), true, null));
        assertEquals("PENDING", announcement.get("status"), "Administrators also follow school premoderation");
        fails(403, () -> posts.review(manager, id(announcement), new Review("APPROVE", "", version(announcement))));
        fails(403, () -> posts.review(outsider, id(announcement), new Review("APPROVE", "", version(announcement))));
        var approved = posts.review(secondManager, id(announcement), new Review("APPROVE", "", version(announcement)));
        jdbc.update("UPDATE campus_membership SET role='MEMBER',version=version+1 WHERE school_id=? AND user_id=?", school, manager);
        fails(403, () -> posts.pin(manager, id(announcement), new Pin(true, version(approved))));
        assertEquals(false, posts.get(peer, id(announcement)).get("pinned"));
        assertEquals("PUBLISHED", posts.get(siteAdmin, id(announcement)).get("status"));
    }

    @Test
    void feedSearchPagingAndBookmarksFilterBeforeCountingAndNeverEnterPublicArticles() {
        immediatePublishing();
        var literal = create(author, "10%_!", "literal only", true);
        create(author, "10anything", "other text", true);
        create(author, "secret draft", "hidden needle", false);
        jdbc.update("UPDATE campus_school SET pre_moderation=FALSE WHERE id=?", otherSchool);
        posts.create(outsider, otherSchool, write("10%_!", "other school", true, null));
        assertEquals(List.of(id(literal)), list(peer, "feed", "10%_!", null, 0, 20).list().stream().map(CampusPostTestSupport::id).toList());
        assertEquals(2, list(peer, "feed", null, null, 0, 1).total());
        assertEquals(1, list(peer, "feed", null, null, 1, 1).list().size());
        assertEquals(0, list(peer, "feed", null, null, 2, 1).list().size());
        assertEquals(2, list(peer, "feed", null, null, 2, 1).total());
        posts.bookmark(peer, id(literal), true); posts.bookmark(peer, id(literal), true);
        assertEquals(1, list(peer, "bookmarks", null, null, 0, 20).total());
        posts.update(author, id(literal), write("now private", "not for bookmarks", false, version(literal)));
        assertEquals(0, list(peer, "bookmarks", null, null, 0, 20).total());
        assertEquals(0, count("SELECT COUNT(*) FROM articles WHERE user_id BETWEEN ? AND ?", author, secondManager));
        fails(403, () -> list(outsider, "feed", null, null, 0, 20));
        fails(400, () -> list(peer, "feed", null, "DRAFT", 0, 20));
        fails(400, () -> list(peer, "feed", "x".repeat(101), null, 0, 20));
        fails(400, () -> list(peer, "feed", null, null, 100, 100));
        fails(400, () -> posts.list(peer, school, null, null, "id; DROP TABLE campus_post", "feed", null, 0, 20));
    }

    @Test
    void concurrentRepeatedLikesCreateOneRowOneNotificationAndStablePopularOrder() throws Exception {
        immediatePublishing();
        var first = create(author, "first", "published first", true);
        var second = create(author, "second", "published second", true);
        var start = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(2);
        try {
            Callable<Map<String, Object>> like = () -> { start.await(); return posts.like(peer, id(first), true); };
            Future<Map<String, Object>> a = workers.submit(like), b = workers.submit(like); start.countDown();
            assertEquals(1L, a.get(10, TimeUnit.SECONDS).get("likeCount"));
            assertEquals(1L, b.get(10, TimeUnit.SECONDS).get("likeCount"));
        } finally { workers.shutdownNow(); }
        assertEquals(1, count("SELECT COUNT(*) FROM campus_post_like WHERE post_id=?", id(first)));
        assertEquals(1, count("SELECT COUNT(*) FROM notifications WHERE entity_id=? AND type='CAMPUS_LIKE'", id(first)));
        assertEquals(id(first), id(posts.list(peer, school, null, null, "popular", "feed", null, 0, 20).list().get(0)));
        var pinned = posts.pin(manager, id(second), new Pin(true, version(second)));
        assertEquals(id(second), id(posts.list(peer, school, null, null, "popular", "feed", null, 0, 20).list().get(0)));
        assertEquals(0L, posts.like(peer, id(first), false).get("likeCount"));
        assertEquals(0L, posts.like(peer, id(first), false).get("likeCount"));
        posts.bookmark(peer, id(first), true); posts.bookmark(peer, id(first), true);
        assertEquals(1, count("SELECT COUNT(*) FROM campus_post_bookmark WHERE post_id=?", id(first)));
        posts.bookmark(peer, id(first), false); posts.bookmark(peer, id(first), false);
        assertEquals(0, count("SELECT COUNT(*) FROM campus_post_bookmark WHERE post_id=?", id(first)));
        assertEquals(true, pinned.get("pinned"));
    }

    @Test
    void replyOwnershipAndDeletedTombstonesPreserveRelationsWithoutOldContent() {
        immediatePublishing();
        var post = create(author, "thread", "thread body", true);
        var another = create(author, "another", "another body", true);
        var parent = posts.comment(peer, id(post), new CommentWrite("private comment text", null));
        var reply = posts.comment(author, id(post), new CommentWrite("reply", id(parent)));
        fails(404, () -> posts.comment(peer, id(another), new CommentWrite("cross post", id(parent))));
        fails(404, () -> posts.deleteComment(peer, id(another), id(parent)));
        fails(403, () -> posts.deleteComment(author, id(post), id(parent)));
        posts.deleteComment(peer, id(post), id(parent));
        posts.deleteComment(peer, id(post), id(parent));
        var comments = posts.comments(peer, id(post), 0, 1);
        assertEquals(2, comments.total());
        assertEquals("", comments.list().get(0).get("content"));
        assertEquals(true, comments.list().get(0).get("deleted"));
        assertEquals(id(parent), posts.comments(peer, id(post), 1, 1).list().get(0).get("replyToCommentId"));
        assertEquals(1L, posts.get(peer, id(post)).get("commentCount"));
        assertEquals("", jdbc.queryForObject("SELECT content FROM campus_comment WHERE id=?", String.class, id(parent)));
        fails(409, () -> posts.comment(peer, id(post), new CommentWrite("reply after deletion", id(parent))));
        posts.deleteComment(manager, id(post), id(reply));
        assertEquals(0L, posts.get(peer, id(post)).get("commentCount"));
        fails(403, () -> posts.comments(outsider, id(post), 0, 20));
    }

    @Test
    void reportDismissalAndRemovalAreRealAndRespectSchoolSelfReviewAndVersionBoundaries() {
        immediatePublishing();
        var post = create(author, "reportable", "reported body", true);
        var report = pendingReport(peer, id(post), "需要核查");
        fails(409, () -> posts.report(peer, id(post), new ReportWrite("duplicate")));
        fails(403, () -> posts.decideReport(peer, school, id(report), new Review("REMOVE", "reason", version(report))));
        fails(404, () -> posts.decideReport(outsider, otherSchool, id(report), new Review("REMOVE", "reason", version(report))));
        fails(409, () -> posts.decideReport(manager, school, id(report), new Review("DISMISS", "核查后驳回", 50L)));
        var dismissed = posts.decideReport(manager, school, id(report), new Review("DISMISS", "核查后驳回", version(report)));
        assertEquals("DISMISSED", dismissed.get("status"));
        assertEquals("PUBLISHED", posts.get(peer, id(post)).get("status"));
        var again = pendingReport(peer, id(post), "补充信息");
        var removed = posts.decideReport(manager, school, id(again), new Review("REMOVE", "违规内容已移除", version(again)));
        assertEquals("REMOVED", removed.get("status"));
        assertEquals("REMOVED", jdbc.queryForObject("SELECT status FROM campus_post WHERE id=?", String.class, id(post)));
        assertEquals(version(post) + 1, jdbc.queryForObject("SELECT version FROM campus_post WHERE id=?", Long.class, id(post)));
        assertEquals(1, count("SELECT COUNT(*) FROM campus_audit WHERE school_id=? AND action='REPORT_REMOVE'", school));
        assertEquals(0, list(peer, "feed", "reported body", null, 0, 20).total());
        fails(404, () -> posts.get(manager, id(post)));
        var ownPost = create(manager, "manager post", "manager body", true);
        var ownPostReport = pendingReport(peer, id(ownPost), "review independently");
        fails(403, () -> posts.decideReport(manager, school, id(ownPostReport), new Review("REMOVE", "reason", version(ownPostReport))));
        var peerPost = create(peer, "peer post", "peer body", true);
        var ownReport = pendingReport(manager, id(peerPost), "manager report");
        fails(403, () -> posts.decideReport(manager, school, id(ownReport), new Review("DISMISS", "reason", version(ownReport))));
        assertEquals("DISMISSED", posts.decideReport(secondManager, school, id(ownReport), new Review("DISMISS", "independent", version(ownReport))).get("status"));
    }

    @Test
    void anotherPendingReportCanBeDismissedAfterRemovalWithoutRestoringOrRevisingThePost() {
        immediatePublishing();
        var post = create(author, "two reports", "removed body", true);
        var first = pendingReport(peer, id(post), "first report");
        posts.report(secondManager, id(post), new ReportWrite("independent second report"));
        var second = posts.reports(manager, school, "PENDING", 0, 20).list().stream()
                .filter(row -> ((Number) row.get("reporterId")).longValue() == secondManager).findFirst().orElseThrow();
        posts.decideReport(manager, school, id(first), new Review("REMOVE", "confirmed violation", version(first)));
        fails(404, () -> posts.get(manager, id(post)));
        fails(404, () -> posts.like(peer, id(post), true));
        fails(403, () -> posts.decideReport(secondManager, school, id(second), new Review("DISMISS", "own report", version(second))));
        var dismissed = posts.decideReport(manager, school, id(second), new Review("DISMISS", "already removed", version(second)));
        assertEquals("DISMISSED", dismissed.get("status"));
        assertEquals("REMOVED", jdbc.queryForObject("SELECT status FROM campus_post WHERE id=?", String.class, id(post)));
        assertEquals(version(post) + 1, jdbc.queryForObject("SELECT version FROM campus_post WHERE id=?", Long.class, id(post)));
        assertEquals(0, posts.reports(manager, school, "PENDING", 0, 20).total());
    }

    @Test
    void failedAttachmentAndModerationAuditRollBackAllBusinessRows() {
        immediatePublishing();
        String foreignImage = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO campus_media(id,school_id,owner_id,width,height,size) VALUES (?,?,?,1,1,10)", foreignImage, school, peer);
        fails(400, () -> posts.create(author, school, new PostWrite("rolled back", "must not persist", "GENERAL", List.of(foreignImage), true, null)));
        assertEquals(0, count("SELECT COUNT(*) FROM campus_post WHERE school_id=?", school));
        assertEquals(0, count("SELECT COUNT(*) FROM campus_audit WHERE school_id=?", school));
        var post = create(author, "original", "original body", true);
        fails(400, () -> posts.update(author, id(post), new PostWrite("changed", "changed body", "GENERAL", List.of(foreignImage), false, version(post))));
        assertEquals("original body", posts.get(peer, id(post)).get("content"));
        assertEquals(version(post), posts.get(peer, id(post)).get("version"));
        var report = pendingReport(peer, id(post), "report");
        doThrow(new IllegalStateException("fixture audit write failed")).when(audit).record(eq(manager), eq(school), eq("REPORT_REMOVE"), anyString(), anyString(), anyString());
        assertThrows(IllegalStateException.class, () -> posts.decideReport(manager, school, id(report), new Review("REMOVE", "reason", version(report))));
        assertEquals("PUBLISHED", posts.get(peer, id(post)).get("status"));
        assertEquals("PENDING", posts.reports(manager, school, "PENDING", 0, 20).list().get(0).get("status"));
        assertEquals(0, count("SELECT COUNT(*) FROM campus_audit WHERE school_id=? AND action='POST_REMOVED'", school));
    }

    @Test
    void currentMembershipOverridesAnOldRepeatableReadSnapshotBeforeMutation() throws Exception {
        immediatePublishing();
        var post = create(author, "revocation", "private campus body", true);
        posts.bookmark(peer, id(post), true);
        var snapshot = new TransactionTemplate(transactions);
        snapshot.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var worker = Executors.newSingleThreadExecutor();
        try {
            snapshot.executeWithoutResult(status -> {
                assertEquals("VERIFIED", jdbc.queryForObject("SELECT status FROM campus_membership WHERE school_id=? AND user_id=?", String.class, school, peer));
                try {
                    worker.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                        access.lockSchool(manager, school);
                        jdbc.update("UPDATE campus_membership SET status='SUSPENDED',version=version+1 WHERE school_id=? AND user_id=?", school, peer);
                    })).get(10, TimeUnit.SECONDS);
                } catch (Exception failure) { throw new AssertionError(failure); }
                assertEquals("VERIFIED", jdbc.queryForObject("SELECT status FROM campus_membership WHERE school_id=? AND user_id=?", String.class, school, peer));
                fails(403, () -> posts.like(peer, id(post), true));
                status.setRollbackOnly();
            });
        } finally { worker.shutdownNow(); }
        assertEquals(0, count("SELECT COUNT(*) FROM campus_post_like WHERE post_id=?", id(post)));
        fails(403, () -> posts.get(peer, id(post)));
        fails(403, () -> list(peer, "bookmarks", null, null, 0, 20));
        jdbc.update("UPDATE campus_school SET active=FALSE WHERE id=?", school);
        fails(403, () -> posts.get(author, id(post)));
        fails(403, () -> posts.pin(siteAdmin, id(post), new Pin(true, version(post))));
    }

    @Test
    void failedReviewNotificationRollsBackReviewAndDoesNotPublishBeforeCommit() {
        var post = create(author, "notification", "pending body", true);
        var failing = spy(notifications);
        doAnswer(invocation -> { invocation.callRealMethod(); throw new IllegalStateException("fixture failure after inbox insert"); })
                .when(failing).send(eq(manager), eq(author), eq("CAMPUS_REVIEW"), eq("campus_post"), eq(id(post)));
        var target = new CampusPostService(jdbc, access, media, failing, audit);
        var proxy = new org.springframework.aop.framework.ProxyFactory(target);
        proxy.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(transactions,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        var failingPosts = (CampusPostService) proxy.getProxy();
        assertThrows(IllegalStateException.class, () -> failingPosts.review(manager, id(post), new Review("APPROVE", "", version(post))));
        assertEquals("PENDING", posts.get(author, id(post)).get("status"));
        assertEquals(version(post), posts.get(author, id(post)).get("version"));
        assertEquals(0, count("SELECT COUNT(*) FROM notifications WHERE entity_id=? AND type='CAMPUS_REVIEW'", id(post)));
        verifyNoInteractions(transport);
    }
}
