package com.web.campus;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.web.campus.CampusSchoolDtos.*;

@RestController
@RequestMapping("/api/campus/schools")
public class CampusSchoolController {
    private final CampusSchoolService schools;

    public CampusSchoolController(CampusSchoolService schools) { this.schools = schools; }

    @GetMapping
    public ApiResponse<CampusPage<School>> list(@Userid Long actor, @RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "false") boolean mine,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(schools.listSchools(actor(actor), q, mine, page, size));
    }

    @PostMapping
    public ApiResponse<School> create(@Userid Long actor, @RequestBody CreateSchool request) {
        return ApiResponse.success(schools.createSchool(actor(actor), request));
    }

    @GetMapping("/{schoolId}")
    public ApiResponse<School> get(@Userid Long actor, @PathVariable long schoolId) {
        return ApiResponse.success(schools.getSchool(actor(actor), schoolId));
    }

    @PutMapping("/{schoolId}")
    public ApiResponse<School> update(@Userid Long actor, @PathVariable long schoolId, @RequestBody UpdateSchool request) {
        return ApiResponse.success(schools.updateSchool(actor(actor), schoolId, request));
    }

    @GetMapping("/{schoolId}/applications/mine")
    public ApiResponse<CampusPage<Application>> mine(@Userid Long actor, @PathVariable long schoolId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(schools.myApplications(actor(actor), schoolId, page, size));
    }

    @PostMapping("/{schoolId}/applications")
    public ApiResponse<Application> apply(@Userid Long actor, @PathVariable long schoolId, @RequestBody Apply request) {
        return ApiResponse.success(schools.apply(actor(actor), schoolId, request));
    }

    @DeleteMapping("/{schoolId}/applications/{id}")
    public ApiResponse<Map<String, Boolean>> cancel(@Userid Long actor, @PathVariable long schoolId, @PathVariable long id) {
        schools.cancelApplication(actor(actor), schoolId, id);
        return success();
    }

    @GetMapping("/{schoolId}/applications")
    public ApiResponse<CampusPage<Application>> applications(@Userid Long actor, @PathVariable long schoolId,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(schools.applications(actor(actor), schoolId, status, page, size));
    }

    @PutMapping("/{schoolId}/applications/{id}/review")
    public ApiResponse<Application> review(@Userid Long actor, @PathVariable long schoolId, @PathVariable long id,
                                           @RequestBody Review request) {
        return ApiResponse.success(schools.reviewApplication(actor(actor), schoolId, id, request));
    }

    @DeleteMapping("/{schoolId}/membership")
    public ApiResponse<Map<String, Boolean>> leave(@Userid Long actor, @PathVariable long schoolId) {
        schools.leave(actor(actor), schoolId);
        return success();
    }

    @GetMapping("/{schoolId}/members")
    public ApiResponse<CampusPage<Member>> members(@Userid Long actor, @PathVariable long schoolId,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(schools.members(actor(actor), schoolId, q, status, page, size));
    }

    @PutMapping("/{schoolId}/members/{userId}")
    public ApiResponse<Member> member(@Userid Long actor, @PathVariable long schoolId, @PathVariable long userId,
                                     @RequestBody UpdateMember request) {
        return ApiResponse.success(schools.updateMember(actor(actor), schoolId, userId, request));
    }

    @GetMapping("/{schoolId}/audit")
    public ApiResponse<CampusPage<Audit>> audit(@Userid Long actor, @PathVariable long schoolId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(schools.audit(actor(actor), schoolId, page, size));
    }

    private static long actor(Long actor) {
        if (actor == null || actor <= 0) throw new CampusException(401, "请先登录");
        return actor;
    }

    private static ApiResponse<Map<String, Boolean>> success() { return ApiResponse.success(Map.of("success", true)); }
}
