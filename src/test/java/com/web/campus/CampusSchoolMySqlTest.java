package com.web.campus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.web.campus.CampusSchoolDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
class CampusSchoolMySqlTest extends CampusSchoolTestSupport {
    @Test
    void creationRequiresPersistedSiteAdminAndDirectorySearchIsLiteralAndPrivate() throws Exception {
        failure(403, () -> schools.createSchool(applicant, new CreateSchool("Denied", "", true)));
        var created = schools.createSchool(siteAdmin, new CreateSchool("Literal %_ " + school, "School description", null));
        ownedSchools.add(created.id());
        assertEquals("ADMIN", created.membership().role());
        assertTrue(created.preModeration());
        assertTrue(created.capabilities().canRead());
        assertEquals(1, created.memberCount());
        failure(409, () -> schools.createSchool(siteAdmin, new CreateSchool(created.name(), "", true)));
        var found = schools.listSchools(applicant, "%_ " + school, false, 0, 20);
        assertEquals(List.of(created.id()), found.list().stream().map(School::id).toList());
        assertEquals(1, found.total());
        assertEquals(0, schools.listSchools(applicant, null, true, 0, 20).total());
        var payload = new ObjectMapper().findAndRegisterModules().writeValueAsString(found);
        for (String sensitive : List.of("password", "user_email", "realName", "studentNumber", "department", "statement")) {
            assertFalse(payload.contains(sensitive));
        }
        assertFalse(found.list().get(0).capabilities().canRead());
    }

    @Test
    void applicationRejectResubmitApproveUsesPrivateHistoryAndNeverGrantsGlobalAuthority() throws Exception {
        var first = schools.apply(applicant, school, applicationInput());
        assertEquals("PENDING", first.status());
        failure(403, () -> access.requireRead(applicant, school));
        failure(409, () -> schools.apply(applicant, school, applicationInput()));
        failure(403, () -> schools.applications(outsider, school, null, 0, 20));
        assertEquals(0, schools.myApplications(outsider, school, 0, 20).total());
        assertEquals(1, schools.myApplications(applicant, school, 0, 20).total());
        assertNull(schools.getSchool(outsider, school).latestApplication());
        var summary = new ObjectMapper().findAndRegisterModules().writeValueAsString(schools.getSchool(applicant, school));
        assertFalse(summary.contains(first.realName()));
        assertFalse(summary.contains(first.studentNumber()));
        assertFalse(summary.contains(first.statement()));
        var rejected = schools.reviewApplication(manager, school, first.id(), new Review("REJECT", "Please clarify affiliation", 1L));
        assertEquals("REJECTED", rejected.status());
        assertEquals(2, rejected.version());
        failure(409, () -> schools.reviewApplication(manager, school, first.id(), new Review("APPROVE", "", 1L)));
        var second = schools.apply(applicant, school, applicationInput());
        assertNotEquals(first.id(), second.id());
        var approved = schools.reviewApplication(manager, school, second.id(), new Review("APPROVE", "Reviewed manually", 1L));
        assertEquals("APPROVED", approved.status());
        assertTrue(access.requireRead(applicant, school).member());
        assertEquals("MEMBER", schools.getSchool(applicant, school).membership().role());
        assertEquals("USER", jdbc.queryForObject("SELECT type FROM `user` WHERE id=?", String.class, applicant));
        assertEquals(2, schools.myApplications(applicant, school, 0, 20).total());
        assertEquals(second.id(), schools.myApplications(applicant, school, 0, 20).list().get(0).id());
        var members = new ObjectMapper().findAndRegisterModules().writeValueAsString(schools.members(manager, school, null, null, 0, 20));
        assertFalse(members.contains(approved.realName()));
        assertFalse(members.contains(approved.studentNumber()));
        var audit = schools.audit(manager, school, 0, 100);
        assertFalse(audit.list().stream().anyMatch(row -> row.details().contains(first.studentNumber()) || row.details().contains(first.realName())));
        verify(notifications, times(2)).send(manager, applicant, "CAMPUS_VERIFICATION", "campus_school", school);
    }

    @Test
    void cancellationIsOwnerBoundAndApplicationsCannotBeReviewedThroughAnotherSchool() {
        var application = schools.apply(applicant, school, applicationInput());
        failure(403, () -> schools.cancelApplication(outsider, school, application.id()));
        failure(403, () -> schools.reviewApplication(secondManager, school, application.id(), new Review("APPROVE", "", 1L)));
        failure(404, () -> schools.reviewApplication(siteAdmin, secondSchool, application.id(), new Review("APPROVE", "", 1L)));
        schools.cancelApplication(applicant, school, application.id());
        var cancelled = schools.myApplications(applicant, school, 0, 20).list().get(0);
        assertEquals("CANCELLED", cancelled.status());
        assertEquals(2, cancelled.version());
        failure(409, () -> schools.reviewApplication(manager, school, application.id(), new Review("APPROVE", "", 2L)));
        assertEquals("PENDING", schools.apply(applicant, school, applicationInput()).status());
    }

    @Test
    void selfReviewIsForbiddenEvenForSiteAdminAndFailedReviewLeavesNoMembership() {
        var own = schools.apply(siteAdmin, school, applicationInput());
        failure(403, () -> schools.reviewApplication(siteAdmin, school, own.id(), new Review("APPROVE", "", 1L)));
        assertEquals(0, value("SELECT COUNT(*) FROM campus_membership WHERE school_id=? AND user_id=?", school, siteAdmin));
        assertEquals("PENDING", schools.myApplications(siteAdmin, school, 0, 20).list().get(0).status());
        var target = schools.apply(applicant, school, applicationInput());
        jdbc.update("UPDATE `user` SET status=0 WHERE id=?", applicant);
        failure(409, () -> schools.reviewApplication(manager, school, target.id(), new Review("APPROVE", "", 1L)));
        assertEquals(0, value("SELECT COUNT(*) FROM campus_membership WHERE school_id=? AND user_id=?", school, applicant));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM campus_verification_application WHERE id=?", String.class, target.id()));
    }

    @Test
    void concurrentReviewsCommitExactlyOneDecisionAndMembership() throws Exception {
        var application = schools.apply(applicant, school, applicationInput());
        clearInvocations(notifications);
        var results = race(() -> reviewStatus(manager, application.id()), () -> reviewStatus(siteAdmin, application.id()));
        assertEquals(List.of(200, 409), results.stream().sorted().toList());
        assertEquals(1, value("SELECT COUNT(*) FROM campus_membership WHERE school_id=? AND user_id=? AND status='VERIFIED'", school, applicant));
        assertEquals(1, value("SELECT COUNT(*) FROM campus_audit WHERE school_id=? AND action='APPLICATION_APPROVED'", school));
        verify(notifications, times(1)).send(anyLong(), eq(applicant), eq("CAMPUS_VERIFICATION"), eq("campus_school"), eq(school));
    }

    @Test
    void concurrentSubmissionsCannotCreateTwoPendingApplications() throws Exception {
        Callable<Integer> submit = () -> {
            try { schools.apply(applicant, school, applicationInput()); return 200; }
            catch (CampusException conflict) { return conflict.getStatus(); }
        };
        assertEquals(List.of(200, 409), race(submit, submit).stream().sorted().toList());
        assertEquals(1, value("SELECT COUNT(*) FROM campus_verification_application WHERE school_id=? AND user_id=? AND status='PENDING'", school, applicant));
    }

    @Test
    void suspensionCannotBeBypassedByLeaveOrNewApplicationAndStaleRestoreConflicts() {
        membership(school, applicant, "VERIFIED", "MEMBER");
        var suspended = schools.updateMember(manager, school, applicant, new UpdateMember("SUSPENDED", "MEMBER", "Policy review", 1L));
        assertEquals("SUSPENDED", suspended.status());
        failure(403, () -> access.requireRead(applicant, school));
        failure(409, () -> schools.leave(applicant, school));
        failure(409, () -> schools.apply(applicant, school, applicationInput()));
        failure(409, () -> schools.updateMember(manager, school, applicant, new UpdateMember("VERIFIED", "MEMBER", "Restored", 1L)));
        var restored = schools.updateMember(manager, school, applicant, new UpdateMember("VERIFIED", "MEMBER", "Restored", 2L));
        assertEquals(3, restored.version());
        assertTrue(access.requireRead(applicant, school).member());
        schools.leave(applicant, school);
        assertEquals("LEFT", schools.getSchool(applicant, school).membership().status());
        assertEquals("PENDING", schools.apply(applicant, school, applicationInput()).status());
    }

    @Test
    void campusAdministratorsCannotPromoteModifySelfOrManageAnotherAdministrator() {
        membership(school, applicant, "VERIFIED", "MEMBER");
        failure(403, () -> schools.updateMember(manager, school, applicant, new UpdateMember("VERIFIED", "ADMIN", "", 1L)));
        failure(403, () -> schools.updateMember(manager, school, manager, new UpdateMember("VERIFIED", "ADMIN", "", 1L)));
        failure(403, () -> schools.updateMember(secondManager, school, applicant, new UpdateMember("SUSPENDED", "MEMBER", "Denied", 1L)));
        var promoted = schools.updateMember(siteAdmin, school, applicant, new UpdateMember("VERIFIED", "ADMIN", "Appointed", 1L));
        assertEquals("ADMIN", promoted.role());
        failure(403, () -> schools.updateMember(manager, school, applicant, new UpdateMember("SUSPENDED", "ADMIN", "Denied", 2L)));
        var demoted = schools.updateMember(siteAdmin, school, applicant, new UpdateMember("VERIFIED", "MEMBER", "Role ended", 2L));
        assertEquals("MEMBER", demoted.role());
        failure(403, () -> schools.applications(applicant, school, null, 0, 20));
    }

    @Test
    void lastEnabledCampusAdministratorCannotLeaveBeSuspendedOrBeDemoted() {
        failure(409, () -> schools.leave(manager, school));
        failure(409, () -> schools.updateMember(siteAdmin, school, manager, new UpdateMember("SUSPENDED", "ADMIN", "Denied", 1L)));
        failure(409, () -> schools.updateMember(siteAdmin, school, manager, new UpdateMember("VERIFIED", "MEMBER", "Denied", 1L)));
        membership(school, disabled, "VERIFIED", "ADMIN");
        failure(409, () -> schools.leave(manager, school));
        membership(school, secondManager, "VERIFIED", "ADMIN");
        schools.leave(manager, school);
        assertEquals("LEFT", schools.getSchool(manager, school).membership().status());
        assertEquals("MEMBER", schools.getSchool(manager, school).membership().role());
        failure(403, () -> schools.members(manager, school, null, null, 0, 20));
    }

    @Test
    void schoolSettingsHonorVersionScopedFieldsAndInactiveVisibility() {
        failure(403, () -> schools.updateSchool(manager, school, new UpdateSchool("Renamed", null, null, null, 1L)));
        failure(403, () -> schools.updateSchool(manager, school, new UpdateSchool(null, null, null, false, 1L)));
        var updated = schools.updateSchool(manager, school, new UpdateSchool(null, "Updated description", false, null, 1L));
        assertFalse(updated.preModeration());
        assertEquals(2, updated.version());
        failure(409, () -> schools.updateSchool(manager, school, new UpdateSchool(null, "Stale", true, null, 1L)));
        var application = schools.apply(applicant, school, applicationInput());
        var inactive = schools.updateSchool(siteAdmin, school, new UpdateSchool(null, null, null, false, 2L));
        assertFalse(inactive.active());
        assertTrue(schools.getSchool(manager, school).capabilities().canManageSchool());
        assertFalse(schools.getSchool(manager, school).capabilities().canModerate());
        failure(403, () -> schools.getSchool(applicant, school));
        assertTrue(schools.listSchools(applicant, Long.toString(school), false, 0, 20).list().isEmpty());
        failure(403, () -> schools.applications(manager, school, null, 0, 20));
        failure(403, () -> schools.reviewApplication(manager, school, application.id(), new Review("APPROVE", "", 1L)));
        assertEquals(1, schools.myApplications(applicant, school, 0, 20).total());
        schools.cancelApplication(applicant, school, application.id());
        assertEquals("CANCELLED", schools.myApplications(applicant, school, 0, 20).list().get(0).status());
    }

    @Test
    void notificationWriteFailureRollsBackApprovalMembershipAndAuditTogether() {
        var application = schools.apply(applicant, school, applicationInput());
        doThrow(new IllegalStateException("simulated inbox failure")).when(notifications)
                .send(manager, applicant, "CAMPUS_VERIFICATION", "campus_school", school);
        assertThrows(IllegalStateException.class, () -> schools.reviewApplication(manager, school, application.id(), new Review("APPROVE", "", 1L)));
        assertEquals("PENDING", schools.myApplications(applicant, school, 0, 20).list().get(0).status());
        assertEquals(0, value("SELECT COUNT(*) FROM campus_membership WHERE school_id=? AND user_id=?", school, applicant));
        assertEquals(0, value("SELECT COUNT(*) FROM campus_audit WHERE school_id=? AND action='APPLICATION_APPROVED'", school));
    }

    private int reviewStatus(long actor, long id) {
        try { schools.reviewApplication(actor, school, id, new Review("APPROVE", "", 1L)); return 200; }
        catch (CampusException conflict) { return conflict.getStatus(); }
    }

    private List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(2);
        try {
            var a = workers.submit(() -> { ready.countDown(); assertTrue(go.await(5, TimeUnit.SECONDS)); return first.call(); });
            var b = workers.submit(() -> { ready.countDown(); assertTrue(go.await(5, TimeUnit.SECONDS)); return second.call(); });
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            go.countDown();
            return List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
