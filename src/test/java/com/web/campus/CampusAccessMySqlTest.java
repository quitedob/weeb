package com.web.campus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.web.campus.CampusSchoolDtos.UpdateMember;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
class CampusAccessMySqlTest extends CampusSchoolTestSupport {
    @Test
    void persistedEnabledGlobalRoleAndScopedVerifiedMembershipAreIndependent() {
        assertTrue(access.isSiteAdmin(siteAdmin));
        assertFalse(access.isSiteAdmin(disabled));
        assertFalse(access.isSiteAdmin(manager));
        assertTrue(access.inspect(manager, school).manager());
        assertFalse(access.inspect(manager, secondSchool).manager());
        assertTrue(access.requireRead(siteAdmin, secondSchool).siteAdmin());
        assertFalse(access.requireRead(siteAdmin, secondSchool).member());
        failure(401, () -> access.inspect(disabled, school));
        membership(school, applicant, "SUSPENDED", "ADMIN");
        assertFalse(access.inspect(applicant, school).manager());
        assertFalse(access.inspect(applicant, school).member());
        failure(403, () -> access.requireManage(applicant, school));
        jdbc.update("UPDATE `user` SET type='USER' WHERE id=?", siteAdmin);
        assertFalse(access.isSiteAdmin(siteAdmin));
        failure(403, () -> access.requireRead(siteAdmin, school));
    }

    @Test
    void privatePostStatesRequireCurrentSchoolMembershipAndDoNotUseAuthorshipAsBypass() {
        membership(school, applicant, "VERIFIED", "MEMBER");
        long post = school + 300;
        jdbc.update("INSERT INTO campus_post(id,school_id,author_id,title,content,status) VALUES (?,?,?,'Private','private content','DRAFT')", post, school, applicant);
        assertTrue(access.canReadPost(applicant, post));
        assertTrue(access.canReadPost(manager, post));
        assertTrue(access.canReadPost(siteAdmin, post));
        assertFalse(access.canReadPost(secondManager, post));
        membership(school, outsider, "VERIFIED", "MEMBER");
        assertFalse(access.canReadPost(outsider, post));
        jdbc.update("UPDATE campus_post SET status='PUBLISHED' WHERE id=?", post);
        assertTrue(access.canReadPost(outsider, post));
        schools.leave(applicant, school);
        assertFalse(access.canReadPost(applicant, post));
        jdbc.update("UPDATE campus_post SET status='REMOVED' WHERE id=?", post);
        assertFalse(access.canReadPost(manager, post));
        assertFalse(access.canReadPost(siteAdmin, post));
        failure(404, () -> access.requirePostRead(siteAdmin, post));
    }

    @Test
    void inactiveSchoolDisablesContentEvenForSiteAdminWithoutLosingSettingsAuthority() {
        jdbc.update("UPDATE campus_school SET active=FALSE WHERE id=?", school);
        assertTrue(access.inspect(manager, school).manager());
        assertFalse(access.inspect(manager, school).active());
        failure(403, () -> access.requireRead(manager, school));
        failure(403, () -> access.requireRead(siteAdmin, school));
        failure(403, () -> access.requireManage(siteAdmin, school));
    }

    @Test
    void schoolLockBypassesOldRepeatableReadMembershipSnapshotAfterSuspension() throws Exception {
        membership(school, applicant, "VERIFIED", "MEMBER");
        var staleTransaction = new TransactionTemplate(transactions);
        staleTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var otherConnection = Executors.newSingleThreadExecutor();
        try {
            staleTransaction.executeWithoutResult(status -> {
                assertEquals("VERIFIED", jdbc.queryForObject("SELECT status FROM campus_membership WHERE school_id=? AND user_id=?", String.class, school, applicant));
                try {
                    otherConnection.submit(() -> schools.updateMember(manager, school, applicant,
                            new UpdateMember("SUSPENDED", "MEMBER", "Review pending", 1L))).get(10, TimeUnit.SECONDS);
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
                // The transaction still has its old snapshot, but authorization uses current row locks.
                assertEquals("VERIFIED", jdbc.queryForObject("SELECT status FROM campus_membership WHERE school_id=? AND user_id=?", String.class, school, applicant));
                assertFalse(access.lockSchool(applicant, school).member());
            });
        } finally {
            otherConnection.shutdownNow();
            assertTrue(otherConnection.awaitTermination(10, TimeUnit.SECONDS));
        }
        failure(403, () -> access.requireRead(applicant, school));
    }

    @Test
    void schoolLockRechecksGlobalAdminAfterOldAccountSnapshot() throws Exception {
        var staleTransaction = new TransactionTemplate(transactions);
        staleTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var otherConnection = Executors.newSingleThreadExecutor();
        try {
            staleTransaction.executeWithoutResult(status -> {
                assertTrue(access.isSiteAdmin(siteAdmin));
                try {
                    otherConnection.submit(() -> jdbc.update("UPDATE `user` SET type='USER' WHERE id=?", siteAdmin)).get(10, TimeUnit.SECONDS);
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
                var current = access.lockSchool(siteAdmin, school);
                assertFalse(current.siteAdmin());
                assertFalse(current.manager());
            });
        } finally {
            otherConnection.shutdownNow();
            assertTrue(otherConnection.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
