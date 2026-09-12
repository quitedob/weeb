package com.web.campus;

import com.web.config.UserInfoArgumentResolver;
import com.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static com.web.campus.CampusSchoolDtos.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CampusSchoolControllerTest {
    private CampusSchoolService schools;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        schools = mock(CampusSchoolService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CampusSchoolController(schools))
                .setCustomArgumentResolvers(new UserInfoArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void actorComesFromAuthenticatedRequestNotBodyOrQuery() throws Exception {
        mvc.perform(post("/api/campus/schools/9/applications").requestAttr("userinfo", Map.of("userId", 41L))
                        .param("actor", "999").param("userId", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"realName\":\"Student\",\"studentNumber\":\"123\",\"department\":\"Math\",\"enrollmentYear\":2024,\"statement\":\"\",\"userId\":999,\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        verify(schools).apply(eq(41L), eq(9L), eq(new Apply("Student", "123", "Math", 2024, "")));
        mvc.perform(put("/api/campus/schools/9/members/42").requestAttr("userinfo", Map.of("userId", 41L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\",\"role\":\"MEMBER\",\"reason\":\"Review\",\"version\":1}"))
                .andExpect(status().isOk());
        verify(schools).updateMember(41, 9, 42, new UpdateMember("SUSPENDED", "MEMBER", "Review", 1L));
    }

    @Test
    void pagingContractAndPrivateHistoryRoutePreservePathAndAuthenticatedActor() throws Exception {
        when(schools.listSchools(41, "school", true, 2, 10)).thenReturn(new CampusPage<>(List.of(), 21, 2, 10));
        mvc.perform(get("/api/campus/schools").requestAttr("userinfo", Map.of("userId", 41L))
                        .param("q", "school").param("mine", "true").param("page", "2").param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(21)).andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(10));
        when(schools.myApplications(41, 9, 0, 20)).thenReturn(new CampusPage<>(List.of(), 0, 0, 20));
        mvc.perform(get("/api/campus/schools/9/applications/mine").requestAttr("userinfo", Map.of("userId", 41L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        verify(schools).myApplications(41, 9, 0, 20);
    }

    @Test
    void missingIdentityAndServiceDenialsRetain401403409InsteadOfSuccessEnvelope() throws Exception {
        mvc.perform(get("/api/campus/schools")).andExpect(status().isUnauthorized());
        verifyNoInteractions(schools);
        when(schools.members(41, 9, null, null, 0, 20)).thenThrow(new CampusException(403, "Forbidden"));
        mvc.perform(get("/api/campus/schools/9/members").requestAttr("userinfo", Map.of("userId", 41L)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        doThrow(new CampusException(409, "Last administrator")).when(schools).leave(41, 9);
        mvc.perform(delete("/api/campus/schools/9/membership").requestAttr("userinfo", Map.of("userId", 41L)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409));
    }
}
