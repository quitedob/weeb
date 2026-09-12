package com.web.campus;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Year;

import static com.web.campus.CampusSchoolDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CampusSchoolValidationTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CampusAccessService access = mock(CampusAccessService.class);
    private final CampusSchoolService schools = new CampusSchoolService(jdbc, access,
            mock(CampusAuditService.class), mock(CampusNotificationService.class));

    @Test
    void boundedPaginationAndLiteralSearchFailBeforeDatabaseWork() {
        invalid(() -> schools.listSchools(1, "", false, -1, 20));
        invalid(() -> schools.listSchools(1, "", false, 0, 101));
        invalid(() -> schools.listSchools(1, "", false, 500, 20));
        invalid(() -> schools.listSchools(1, "x".repeat(101), false, 0, 20));
        invalid(() -> schools.members(1, 2, "x".repeat(101), null, 0, 20));
        verifyNoInteractions(jdbc, access);
    }

    @Test
    void verificationRejectsMissingOversizedAndInvalidYearFieldsBeforeLocks() {
        invalid(() -> schools.apply(1, 2, null));
        invalid(() -> schools.apply(1, 2, new Apply(" ", "123", "Department", 2024, "")));
        invalid(() -> schools.apply(1, 2, new Apply("Name", "x".repeat(41), "Department", 2024, "")));
        invalid(() -> schools.apply(1, 2, new Apply("Name", "123", null, 2024, "")));
        invalid(() -> schools.apply(1, 2, new Apply("Name", "123", "Department", null, "")));
        invalid(() -> schools.apply(1, 2, new Apply("Name", "123", "Department", 1899, "")));
        invalid(() -> schools.apply(1, 2, new Apply("Name", "123", "Department", Year.now().getValue() + 2, "")));
        invalid(() -> schools.apply(1, 2, new Apply("Name", "123", "Department", 2024, "x".repeat(1001))));
        verifyNoInteractions(jdbc, access);
    }

    @Test
    void staleWriteInputsRequirePositiveVersionAndAllowlistedStates() {
        invalid(() -> schools.updateSchool(1, 2, new UpdateSchool(null, "", true, true, null)));
        invalid(() -> schools.reviewApplication(1, 2, 3, new Review("APPROVE", "", 0L)));
        invalid(() -> schools.reviewApplication(1, 2, 3, new Review("VERIFIED", "", 1L)));
        invalid(() -> schools.reviewApplication(1, 2, 3, new Review("REJECT", " ", 1L)));
        invalid(() -> schools.updateMember(1, 2, 3, new UpdateMember("LEFT", "MEMBER", "", 1L)));
        invalid(() -> schools.updateMember(1, 2, 3, new UpdateMember("VERIFIED", "OWNER", "", 1L)));
        invalid(() -> schools.updateMember(1, 2, 3, new UpdateMember("SUSPENDED", "MEMBER", "", 1L)));
        invalid(() -> schools.applications(1, 2, "ALL", 0, 20));
        invalid(() -> schools.members(1, 2, "", "PENDING", 0, 20));
        verifyNoInteractions(jdbc, access);
    }

    @Test
    void schoolNamesAndDescriptionsUseSchemaBounds() {
        invalid(() -> schools.createSchool(1, new CreateSchool(" ", "", true)));
        invalid(() -> schools.createSchool(1, new CreateSchool("x".repeat(121), "", true)));
        invalid(() -> schools.createSchool(1, new CreateSchool("School", "x".repeat(2001), true)));
        invalid(() -> schools.updateSchool(1, 2, new UpdateSchool("", null, null, null, 1L)));
        verifyNoInteractions(jdbc, access);
    }

    private void invalid(org.junit.jupiter.api.function.Executable call) {
        assertEquals(400, assertThrows(CampusException.class, call).getStatus());
    }
}
