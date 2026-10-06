package io.sala.krob_krong.school.controller;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.*;
import io.sala.krob_krong.iam.security.RequirePermission;
import io.sala.krob_krong.school.dto.*;
import io.sala.krob_krong.school.service.SchoolService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SchoolController {
    private final SchoolService schools;

    @PostMapping("/schools")
    public ApiResponse<SchoolResponse> register(@Valid @RequestBody SchoolProfileRequest request) {
        return ApiResponse.create(schools.register(request));
    }

    @GetMapping("/me/schools")
    public ApiResponse<PageResponse<SchoolSummaryResponse>> mine(PageRequest page) {
        return ApiResponse.success(schools.mine(page));
    }

    @GetMapping("/schools/{school_id}")
    @RequirePermission(value = "school.profile:read", school = "#schoolId")
    public ApiResponse<SchoolResponse> get(@PathVariable("school_id") UUID schoolId) {
        return ApiResponse.success(schools.get(schoolId));
    }

    @PutMapping("/schools/{school_id}/profile")
    @RequirePermission(value = "school.profile:edit", school = "#schoolId")
    public ApiResponse<SchoolResponse> update(
            @PathVariable("school_id") UUID schoolId, @Valid @RequestBody SchoolProfileRequest request) {
        return ApiResponse.success(schools.update(schoolId, request));
    }

    @GetMapping("/schools/{school_id}/onboarding")
    @RequirePermission(value = "school.profile:edit", school = "#schoolId")
    public ApiResponse<OnboardingResponse> onboarding(@PathVariable("school_id") UUID schoolId) {
        return ApiResponse.success(schools.onboarding(schoolId));
    }

    @PostMapping("/schools/{school_id}/submit")
    @RequirePermission(value = "school:submit", school = "#schoolId")
    public ApiResponse<SchoolResponse> submit(@PathVariable("school_id") UUID schoolId) {
        return ApiResponse.success(schools.submit(schoolId));
    }

    @GetMapping("/admin/schools")
    @RequirePermission("school.directory:read")
    public ApiResponse<PageResponse<SchoolResponse>> directory(
            @RequestParam(required = false) String status, PageRequest page) {
        return ApiResponse.success(schools.directory(status, page));
    }
    // Read policy is owner/admin or reviewer, checked by the service.
    @GetMapping("/schools/{school_id}/reviews")
    public ApiResponse<List<SchoolReviewResponse>> reviews(@PathVariable("school_id") UUID schoolId) {
        return ApiResponse.success(schools.reviews(schoolId));
    }

    @PostMapping("/admin/schools/{school_id}/review")
    @RequirePermission("school:review")
    public ApiResponse<SchoolResponse> review(
            @PathVariable("school_id") UUID schoolId, @Valid @RequestBody ReviewDecisionRequest request) {
        return ApiResponse.success(schools.review(schoolId, request));
    }

    @PostMapping("/admin/schools/{school_id}/suspend")
    @RequirePermission("school:suspend")
    public ApiResponse<SchoolResponse> suspend(
            @PathVariable("school_id") UUID schoolId, @Valid @RequestBody StatusReasonRequest request) {
        return ApiResponse.success(schools.suspend(schoolId, request.getReason()));
    }

    @PostMapping("/admin/schools/{school_id}/reinstate")
    @RequirePermission("school:suspend")
    public ApiResponse<SchoolResponse> reinstate(@PathVariable("school_id") UUID schoolId) {
        return ApiResponse.success(schools.reinstate(schoolId));
    }
}
