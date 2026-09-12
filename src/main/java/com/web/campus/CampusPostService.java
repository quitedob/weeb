package com.web.campus;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static com.web.campus.CampusPostDtos.*;

/** Campus content never enters public article tables, search, or public article statistics. */
@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class CampusPostService {
    private static final Set<String> CATEGORIES = Set.of("GENERAL", "STUDY", "LIFE", "LOST_FOUND", "ANNOUNCEMENT");
    private static final Set<String> POST_STATUSES = Set.of("DRAFT", "PENDING", "PUBLISHED", "REJECTED", "REMOVED");
    private static final Set<String> REPORT_STATUSES = Set.of("PENDING", "REMOVED", "DISMISSED");
    private final JdbcTemplate jdbc;
    private final CampusAccessService access;
    private final CampusMediaService media;
    private final CampusNotificationService notifications;
    private final CampusAuditService audit;

    public CampusPostService(JdbcTemplate jdbc, CampusAccessService access, CampusMediaService media,
                             CampusNotificationService notifications, CampusAuditService audit) {
        this.jdbc = jdbc;
        this.access = access;
        this.media = media;
        this.notifications = notifications;
        this.audit = audit;
    }

    public CampusPage<Map<String, Object>> list(long actor, long school, String query, String category,
                                               String sort, String scope, String status, int page, int size) {
        int offset = CampusPage.offset(page, size);
        String keyword = CampusPage.query(query);
        CampusAccessService.Access permission = access.requireRead(actor, school);
        String selectedScope = scope == null ? "feed" : scope;
        if (!Set.of("feed", "mine", "bookmarks").contains(selectedScope)) throw invalid("动态范围无效");
        if (status != null && !"mine".equals(selectedScope)) throw invalid("状态筛选仅用于我的动态");
        String ordering = sort == null ? "latest" : sort;
        if (!Set.of("latest", "popular").contains(ordering)) throw invalid("排序无效");
        StringBuilder where = new StringBuilder("p.school_id=?");
        List<Object> parameters = new ArrayList<>(List.of(school));
        if ("mine".equals(selectedScope)) {
            where.append(" AND p.author_id=?"); parameters.add(actor);
            if (status != null) { enumeration(status, POST_STATUSES); where.append(" AND p.status=?"); parameters.add(status); }
        } else {
            where.append(" AND p.status='PUBLISHED'");
            if ("bookmarks".equals(selectedScope)) {
                where.append(" AND EXISTS(SELECT 1 FROM campus_post_bookmark b WHERE b.post_id=p.id AND b.user_id=?)");
                parameters.add(actor);
            }
        }
        if (category != null) { enumeration(category, CATEGORIES); where.append(" AND p.category=?"); parameters.add(category); }
        if (!keyword.isEmpty()) {
            // Removed body text cannot become a searchable side channel through a tombstone list.
            where.append(" AND p.status<>'REMOVED' AND (LOCATE(LOWER(?),LOWER(p.title))>0 OR LOCATE(LOWER(?),LOWER(p.content))>0)");
            parameters.add(keyword); parameters.add(keyword);
        }
        return postPage(actor, permission, where.toString(), parameters, ordering, page, size, offset);
    }

    public Map<String, Object> get(long actor, long postId) {
        CampusAccessService.Access permission = access.requirePostRead(actor, postId);
        return visiblePost(actor, postId, permission);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> create(long actor, long school, PostWrite request) {
        CampusAccessService.Access permission = access.lockSchool(actor, school);
        readable(permission);
        PostWrite input = postInput(request, permission);
        long id = insert("INSERT INTO campus_post(school_id,author_id,title,content,category,status) VALUES (?,?,?,?,?,?)",
                school, actor, input.title(), input.content(), input.category(), nextStatus(input, permission));
        media.replaceAttachments(actor, school, id, input.mediaIds());
        audit.record(actor, school, "POST_CREATED", "campus_post", Long.toString(id), "");
        return visiblePost(actor, id, permission);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> update(long actor, long postId, PostWrite request) {
        LockedPost locked = lockPost(actor, postId);
        PostRow post = locked.post();
        if (post.author() != actor) throw forbidden();
        existing(post);
        PostWrite input = postInput(request, locked.permission());
        version(input.version(), post.version());
        jdbc.update("UPDATE campus_post SET title=?,content=?,category=?,status=?,version=version+1,pinned=FALSE,"
                        + "review_reason='',reviewed_by=NULL,reviewed_at=NULL,updated_at=CURRENT_TIMESTAMP(3) WHERE id=?",
                input.title(), input.content(), input.category(), nextStatus(input, locked.permission()), postId);
        media.replaceAttachments(actor, post.school(), postId, input.mediaIds());
        audit.record(actor, post.school(), "POST_EDITED", "campus_post", Long.toString(postId), "");
        return visiblePost(actor, postId, locked.permission());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(long actor, long postId, Long expectedVersion) {
        LockedPost locked = lockPost(actor, postId);
        PostRow post = locked.post();
        existing(post);
        if (post.author() != actor && !locked.permission().manager()) throw forbidden();
        version(expectedVersion, post.version());
        removePost(actor, post, post.author() == actor ? "作者删除" : "管理员删除");
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> like(long actor, long postId, boolean desired) {
        LockedPost locked = lockPost(actor, postId); interactive(actor, locked);
        int changed = desired
                ? jdbc.update("INSERT IGNORE INTO campus_post_like(post_id,user_id) VALUES (?,?)", postId, actor)
                : jdbc.update("DELETE FROM campus_post_like WHERE post_id=? AND user_id=?", postId, actor);
        if (desired && changed == 1) notifications.send(actor, locked.post().author(), "CAMPUS_LIKE", "campus_post", postId);
        return Map.of("likedByMe", desired, "likeCount", count("SELECT COUNT(*) FROM campus_post_like WHERE post_id=?", postId));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> bookmark(long actor, long postId, boolean desired) {
        LockedPost locked = lockPost(actor, postId); interactive(actor, locked);
        if (desired) jdbc.update("INSERT IGNORE INTO campus_post_bookmark(post_id,user_id) VALUES (?,?)", postId, actor);
        else jdbc.update("DELETE FROM campus_post_bookmark WHERE post_id=? AND user_id=?", postId, actor);
        return Map.of("bookmarkedByMe", desired);
    }

    public CampusPage<Map<String, Object>> comments(long actor, long postId, int page, int size) {
        int offset = CampusPage.offset(page, size);
        CampusAccessService.Access permission = access.requirePostRead(actor, postId);
        published(post(postId, false));
        var list = jdbc.query(COMMENT_SELECT + " WHERE c.post_id=? AND p.status='PUBLISHED' ORDER BY c.id ASC LIMIT ? OFFSET ?",
                (rs, row) -> commentDto(rs, actor, permission), postId, size, offset);
        long total = count("SELECT COUNT(*) FROM campus_comment c JOIN campus_post p ON p.id=c.post_id WHERE c.post_id=? AND p.status='PUBLISHED'", postId);
        return new CampusPage<>(list, total, page, size);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> comment(long actor, long postId, CommentWrite request) {
        if (request == null) throw invalid("评论不能为空");
        String content = text(request.content(), 1000, "评论");
        LockedPost locked = lockPost(actor, postId); interactive(actor, locked);
        Long replyAuthor = null;
        if (request.replyToCommentId() != null) {
            var parents = jdbc.queryForList("SELECT author_id,deleted FROM campus_comment WHERE id=? AND post_id=? FOR UPDATE",
                    request.replyToCommentId(), postId);
            if (parents.isEmpty()) throw new CampusException(404, "回复的评论不存在于当前动态");
            if (booleanValue(parents.get(0).get("deleted"))) throw new CampusException(409, "该评论已删除");
            replyAuthor = ((Number) parents.get(0).get("author_id")).longValue();
        }
        long id = insert("INSERT INTO campus_comment(post_id,author_id,content,reply_to_comment_id) VALUES (?,?,?,?)",
                postId, actor, content, request.replyToCommentId());
        if (replyAuthor != null) notifications.send(actor, replyAuthor, "CAMPUS_REPLY", "campus_post", postId);
        if (replyAuthor == null || replyAuthor != locked.post().author()) {
            notifications.send(actor, locked.post().author(), "CAMPUS_COMMENT", "campus_post", postId);
        }
        return jdbc.query(COMMENT_SELECT + " WHERE c.id=?", (rs, row) -> commentDto(rs, actor, locked.permission()), id).get(0);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteComment(long actor, long postId, long commentId) {
        LockedPost locked = lockPost(actor, postId); interactive(actor, locked);
        var rows = jdbc.queryForList("SELECT author_id,deleted FROM campus_comment WHERE id=? AND post_id=? FOR UPDATE", commentId, postId);
        if (rows.isEmpty()) throw new CampusException(404, "评论不存在");
        if (((Number) rows.get(0).get("author_id")).longValue() != actor && !locked.permission().manager()) throw forbidden();
        if (!booleanValue(rows.get(0).get("deleted"))) {
            jdbc.update("UPDATE campus_comment SET deleted=TRUE,content='' WHERE id=?", commentId);
            audit.record(actor, locked.post().school(), "COMMENT_DELETED", "campus_comment", Long.toString(commentId), "");
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void report(long actor, long postId, ReportWrite request) {
        if (request == null) throw invalid("举报原因不能为空");
        String reason = text(request.reason(), 500, "举报原因");
        LockedPost locked = lockPost(actor, postId); interactive(actor, locked);
        if (count("SELECT COUNT(*) FROM campus_report WHERE post_id=? AND reporter_id=? AND status='PENDING'", postId, actor) > 0) {
            throw new CampusException(409, "已有待处理举报");
        }
        insert("INSERT INTO campus_report(school_id,post_id,reporter_id,reason) VALUES (?,?,?,?)",
                locked.post().school(), postId, actor, reason);
    }

    public CampusPage<Map<String, Object>> moderationPosts(long actor, long school, String status, int page, int size) {
        int offset = CampusPage.offset(page, size);
        CampusAccessService.Access permission = access.requireManage(actor, school);
        String selected = status == null ? "PENDING" : status; enumeration(selected, POST_STATUSES);
        return postPage(actor, permission, "p.school_id=? AND p.status=?", List.of(school, selected), "latest", page, size, offset);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> review(long actor, long postId, Review request) {
        LockedPost locked = lockPost(actor, postId); manageable(locked.permission());
        PostRow post = locked.post();
        if (post.author() == actor) throw new CampusException(403, "不能审核自己的动态");
        if (request == null) throw invalid("审核请求不能为空");
        enumeration(request.decision(), Set.of("APPROVE", "REJECT"));
        String reason = reason(request.reason(), "REJECT".equals(request.decision()));
        version(request.version(), post.version());
        if (!"PENDING".equals(post.status())) throw new CampusException(409, "仅能审核当前待审核版本");
        jdbc.update("UPDATE campus_post SET status=?,review_reason=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP(3),"
                        + "updated_at=CURRENT_TIMESTAMP(3),version=version+1 WHERE id=?",
                "APPROVE".equals(request.decision()) ? "PUBLISHED" : "REJECTED", reason, actor, postId);
        audit.record(actor, post.school(), "POST_" + request.decision(), "campus_post", Long.toString(postId), reason);
        notifications.send(actor, post.author(), "CAMPUS_REVIEW", "campus_post", postId);
        return visiblePost(actor, postId, locked.permission());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> pin(long actor, long postId, Pin request) {
        LockedPost locked = lockPost(actor, postId); manageable(locked.permission()); published(locked.post());
        if (request == null || request.pinned() == null) throw invalid("置顶设置不能为空");
        version(request.version(), locked.post().version());
        jdbc.update("UPDATE campus_post SET pinned=?,version=version+1,updated_at=CURRENT_TIMESTAMP(3) WHERE id=?", request.pinned(), postId);
        audit.record(actor, locked.post().school(), request.pinned() ? "POST_PINNED" : "POST_UNPINNED", "campus_post", Long.toString(postId), "");
        return visiblePost(actor, postId, locked.permission());
    }

    public CampusPage<Map<String, Object>> reports(long actor, long school, String status, int page, int size) {
        int offset = CampusPage.offset(page, size); access.requireManage(actor, school);
        String selected = status == null ? "PENDING" : status; enumeration(selected, REPORT_STATUSES);
        var rows = jdbc.query(REPORT_SELECT + " WHERE school_id=? AND status=? ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, row) -> reportDto(rs), school, selected, size, offset);
        return new CampusPage<>(rows, count("SELECT COUNT(*) FROM campus_report WHERE school_id=? AND status=?", school, selected), page, size);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> decideReport(long actor, long school, long reportId, Review request) {
        CampusAccessService.Access permission = access.lockSchool(actor, school); manageable(permission);
        var reports = jdbc.query(REPORT_SELECT + " WHERE id=? AND school_id=? FOR UPDATE", (rs, row) -> reportDto(rs), reportId, school);
        if (reports.isEmpty()) throw new CampusException(404, "举报不存在");
        Map<String, Object> report = reports.get(0);
        PostRow post = post(((Number) report.get("postId")).longValue(), true);
        if (post.school() != school) throw new CampusException(404, "举报不存在");
        if (((Number) report.get("reporterId")).longValue() == actor || post.author() == actor) throw new CampusException(403, "不能处理自己的举报或动态");
        if (request == null) throw invalid("处理请求不能为空");
        enumeration(request.decision(), Set.of("REMOVE", "DISMISS"));
        String reason = reason(request.reason(), true);
        version(request.version(), ((Number) report.get("version")).longValue());
        if (!"PENDING".equals(report.get("status"))) throw new CampusException(409, "举报已处理");
        if ("REMOVE".equals(request.decision()) && !"REMOVED".equals(post.status())) removePost(actor, post, reason);
        jdbc.update("UPDATE campus_report SET status=?,decision_reason=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP(3),version=version+1 WHERE id=?",
                "REMOVE".equals(request.decision()) ? "REMOVED" : "DISMISSED", reason, actor, reportId);
        audit.record(actor, school, "REPORT_" + request.decision(), "campus_report", Long.toString(reportId), reason);
        return jdbc.query(REPORT_SELECT + " WHERE id=?", (rs, row) -> reportDto(rs), reportId).get(0);
    }

    private LockedPost lockPost(long actor, long id) {
        PostRow beforeLock = post(id, false);
        CampusAccessService.Access permission = access.lockSchool(actor, beforeLock.school());
        readable(permission);
        PostRow current = post(id, true);
        if (current.school() != permission.schoolId()) throw new CampusException(409, "动态所属学校发生变化");
        return new LockedPost(current, permission);
    }

    private PostRow post(long id, boolean lock) {
        if (id <= 0) throw invalid("动态ID无效");
        var rows = jdbc.query("SELECT id,school_id,author_id,status,version FROM campus_post WHERE id=?" + (lock ? " FOR UPDATE" : ""),
                (rs, row) -> new PostRow(rs.getLong("id"), rs.getLong("school_id"), rs.getLong("author_id"), rs.getString("status"), rs.getLong("version")), id);
        if (rows.isEmpty()) throw new CampusException(404, "动态不存在");
        return rows.get(0);
    }

    private void removePost(long actor, PostRow post, String reason) {
        jdbc.update("UPDATE campus_post SET status='REMOVED',pinned=FALSE,review_reason=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP(3),"
                + "updated_at=CURRENT_TIMESTAMP(3),version=version+1 WHERE id=?", reason, actor, post.id());
        audit.record(actor, post.school(), "POST_REMOVED", "campus_post", Long.toString(post.id()), reason);
    }

    private Map<String, Object> visiblePost(long actor, long id, CampusAccessService.Access permission) {
        var rows = queryPosts(actor, permission, "p.id=? AND p.status<>'REMOVED' AND (p.status='PUBLISHED' OR p.author_id=? OR ?=TRUE)",
                List.of(id, actor, permission.manager()), "p.id DESC", 0, 1);
        if (rows.isEmpty()) throw new CampusException(404, "动态不存在或已不可见");
        return rows.get(0);
    }

    private CampusPage<Map<String, Object>> postPage(long actor, CampusAccessService.Access permission, String where,
                                                    List<?> parameters, String sort, int page, int size, int offset) {
        long total = count("SELECT COUNT(*) FROM campus_post p WHERE " + where, parameters.toArray());
        String order = "p.pinned DESC," + ("popular".equals(sort) ? "like_count DESC," : "") + "p.created_at DESC,p.id DESC";
        return new CampusPage<>(queryPosts(actor, permission, where, parameters, order, offset, size), total, page, size);
    }

    private List<Map<String, Object>> queryPosts(long actor, CampusAccessService.Access permission, String where,
                                                List<?> filters, String order, int offset, int size) {
        var parameters = new ArrayList<Object>(List.of(actor, actor)); parameters.addAll(filters); parameters.add(size); parameters.add(offset);
        return jdbc.query(POST_SELECT + " WHERE " + where + " ORDER BY " + order + " LIMIT ? OFFSET ?",
                (rs, row) -> postDto(rs, actor, permission), parameters.toArray());
    }

    private Map<String, Object> postDto(ResultSet rs, long actor, CampusAccessService.Access permission) throws SQLException {
        long id = rs.getLong("id"), author = rs.getLong("author_id");
        boolean removed = "REMOVED".equals(rs.getString("status"));
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", id); value.put("schoolId", rs.getLong("school_id")); value.put("schoolName", rs.getString("school_name"));
        value.put("author", authorDto(rs)); value.put("title", removed ? "" : rs.getString("title")); value.put("content", removed ? "" : rs.getString("content"));
        value.put("category", rs.getString("category")); value.put("status", rs.getString("status")); value.put("version", rs.getLong("version"));
        value.put("pinned", !removed && rs.getBoolean("pinned")); value.put("reviewReason", rs.getString("review_reason"));
        value.put("images", removed ? List.of() : media.listForPost(id));
        value.put("likeCount", removed ? 0L : rs.getLong("like_count")); value.put("commentCount", removed ? 0L : rs.getLong("comment_count"));
        value.put("likedByMe", !removed && rs.getBoolean("liked_by_me")); value.put("bookmarkedByMe", !removed && rs.getBoolean("bookmarked_by_me"));
        value.put("canEdit", !removed && author == actor); value.put("canDelete", !removed && (author == actor || permission.manager()));
        value.put("canModerate", !removed && permission.manager()); value.put("createdAt", CampusTimestamps.read(rs, "created_at"));
        value.put("updatedAt", CampusTimestamps.read(rs, "updated_at"));
        return value;
    }

    private Map<String, Object> commentDto(ResultSet rs, long actor, CampusAccessService.Access permission) throws SQLException {
        Map<String, Object> value = new LinkedHashMap<>(); boolean deleted = rs.getBoolean("deleted");
        value.put("id", rs.getLong("id")); value.put("postId", rs.getLong("post_id")); value.put("author", authorDto(rs));
        value.put("content", deleted ? "" : rs.getString("content")); value.put("replyToCommentId", rs.getObject("reply_to_comment_id", Long.class));
        value.put("deleted", deleted); value.put("canDelete", !deleted && (actor == rs.getLong("author_id") || permission.manager()));
        value.put("createdAt", CampusTimestamps.read(rs, "created_at")); return value;
    }

    private static Map<String, Object> authorDto(ResultSet rs) throws SQLException {
        Map<String, Object> author = new LinkedHashMap<>(); author.put("id", rs.getLong("author_id"));
        author.put("username", rs.getString("username")); author.put("nickname", rs.getString("nickname")); author.put("avatar", rs.getString("avatar")); return author;
    }

    private static Map<String, Object> reportDto(ResultSet rs) throws SQLException {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", rs.getLong("id")); value.put("schoolId", rs.getLong("school_id")); value.put("postId", rs.getLong("post_id"));
        value.put("reporterId", rs.getLong("reporter_id")); value.put("reason", rs.getString("reason")); value.put("status", rs.getString("status"));
        value.put("decisionReason", rs.getString("decision_reason")); value.put("version", rs.getLong("version"));
        value.put("createdAt", CampusTimestamps.read(rs, "created_at"));
        value.put("reviewedAt", CampusTimestamps.read(rs, "reviewed_at")); return value;
    }

    private PostWrite postInput(PostWrite request, CampusAccessService.Access permission) {
        if (request == null || request.submit() == null) throw invalid("请提供动态内容和提交状态");
        String category = request.category(); enumeration(category, CATEGORIES);
        if ("ANNOUNCEMENT".equals(category) && !permission.manager()) throw forbidden();
        List<String> ids = request.mediaIds() == null ? List.of() : request.mediaIds();
        if (ids.size() > 6 || ids.stream().anyMatch(id -> id == null || id.isBlank()) || new HashSet<>(ids).size() != ids.size()) throw invalid("图片最多6张且不能重复");
        return new PostWrite(text(request.title(), 120, "标题"), text(request.content(), 10_000, "正文"), category, List.copyOf(ids), request.submit(), request.version());
    }

    private static String nextStatus(PostWrite request, CampusAccessService.Access permission) {
        return !request.submit() ? "DRAFT" : permission.preModeration() ? "PENDING" : "PUBLISHED";
    }
    private static void readable(CampusAccessService.Access permission) { if (!permission.active() || (!permission.member() && !permission.siteAdmin())) throw forbidden(); }
    private static void manageable(CampusAccessService.Access permission) { readable(permission); if (!permission.manager()) throw forbidden(); }
    private static void existing(PostRow post) { if ("REMOVED".equals(post.status())) throw new CampusException(404, "动态已移除"); }
    private static void published(PostRow post) { existing(post); if (!"PUBLISHED".equals(post.status())) throw new CampusException(409, "仅已发布动态允许互动"); }
    private static void interactive(long actor, LockedPost locked) {
        existing(locked.post());
        if (!"PUBLISHED".equals(locked.post().status()) && locked.post().author() != actor && !locked.permission().manager()) throw forbidden();
        published(locked.post());
    }
    private static void version(Long expected, long actual) { if (expected == null || expected < 1) throw invalid("请提供有效版本"); if (expected != actual) throw new CampusException(409, "内容已更新，请刷新后重试"); }
    private static void enumeration(String value, Set<String> allowed) { if (value == null || !allowed.contains(value)) throw invalid("枚举值无效"); }
    private static String text(String value, int limit, String label) { if (value == null || value.isBlank() || value.length() > limit) throw invalid(label + "不能为空且不能超过" + limit + "个字符"); return value.trim(); }
    private static String reason(String value, boolean required) { if (required) return text(value, 500, "处理原因"); if (value != null && value.length() > 500) throw invalid("处理原因最多500个字符"); return value == null ? "" : value.trim(); }
    private static boolean booleanValue(Object value) { return Boolean.TRUE.equals(value) || value instanceof Number number && number.intValue() != 0; }
    private static CampusException forbidden() { return new CampusException(403, "无权执行此校园操作"); }
    private static CampusException invalid(String message) { return new CampusException(400, message); }
    private long count(String sql, Object... parameters) { Long value = jdbc.queryForObject(sql, Long.class, parameters); return value == null ? 0 : value; }
    private long insert(String sql, Object... parameters) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> { var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]); return statement; }, keys);
        if (keys.getKey() == null) throw new IllegalStateException("Campus insert did not return its ID"); return keys.getKey().longValue();
    }
    private record PostRow(long id, long school, long author, String status, long version) {}
    private record LockedPost(PostRow post, CampusAccessService.Access permission) {}

    private static final String POST_SELECT = "SELECT p.*,s.name AS school_name,u.username,u.nickname,u.avatar,"
            + CampusTimestamps.select("p", "created_at", "updated_at") + ","
            + "(SELECT COUNT(*) FROM campus_post_like l WHERE l.post_id=p.id) AS like_count,"
            + "(SELECT COUNT(*) FROM campus_comment c WHERE c.post_id=p.id AND c.deleted=FALSE) AS comment_count,"
            + "EXISTS(SELECT 1 FROM campus_post_like l WHERE l.post_id=p.id AND l.user_id=?) AS liked_by_me,"
            + "EXISTS(SELECT 1 FROM campus_post_bookmark b WHERE b.post_id=p.id AND b.user_id=?) AS bookmarked_by_me "
            + "FROM campus_post p JOIN campus_school s ON s.id=p.school_id LEFT JOIN `user` u ON u.id=p.author_id";
    private static final String COMMENT_SELECT = "SELECT c.*,u.username,u.nickname,u.avatar,"
            + CampusTimestamps.select("c", "created_at") + " FROM campus_comment c "
            + "JOIN campus_post p ON p.id=c.post_id LEFT JOIN `user` u ON u.id=c.author_id";
    private static final String REPORT_SELECT = "SELECT r.*," + CampusTimestamps.select("r", "created_at", "reviewed_at")
            + " FROM campus_report r";
}
