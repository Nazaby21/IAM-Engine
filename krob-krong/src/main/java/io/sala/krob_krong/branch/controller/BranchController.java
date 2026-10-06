package io.sala.krob_krong.branch.controller;

import io.sala.krob_krong.branch.dto.*;
import io.sala.krob_krong.branch.service.BranchService;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.iam.security.RequirePermission;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BranchController {
    private final BranchService branches;

    @GetMapping("/schools/{school_id}/branches")
    @RequirePermission(value = "branch:read", school = "#schoolId")
    public ApiResponse<List<BranchResponse>> list(@PathVariable("school_id") UUID schoolId) {
        return ApiResponse.success(branches.list(schoolId));
    }

    @PutMapping("/schools/{school_id}/default-branch")
    @RequirePermission(value = "branch:edit", school = "#schoolId")
    public ApiResponse<BranchResponse> configureDefault(
            @PathVariable("school_id") UUID schoolId, @Valid @RequestBody BranchProfileRequest request) {
        return ApiResponse.success(branches.updateDefault(schoolId, request));
    }

    @PostMapping("/schools/{school_id}/branches")
    @RequirePermission(value = "branch:create", school = "#schoolId")
    public ApiResponse<BranchResponse> add(
            @PathVariable("school_id") UUID schoolId, @Valid @RequestBody BranchProfileRequest request) {
        return ApiResponse.create(branches.add(schoolId, request));
    }

    @PutMapping("/schools/{school_id}/branches/{branch_id}")
    @RequirePermission(value = "branch:edit", school = "#schoolId", branch = "#branchId")
    public ApiResponse<BranchResponse> update(
            @PathVariable("school_id") UUID schoolId,
            @PathVariable("branch_id") UUID branchId,
            @Valid @RequestBody BranchProfileRequest request) {
        return ApiResponse.success(branches.update(schoolId, branchId, request));
    }

    @PostMapping("/schools/{school_id}/branches/{branch_id}/open")
    @RequirePermission(value = "branch:open", school = "#schoolId", branch = "#branchId")
    public ApiResponse<BranchResponse> open(
            @PathVariable("school_id") UUID schoolId, @PathVariable("branch_id") UUID branchId) {
        return ApiResponse.success(branches.open(schoolId, branchId));
    }

    @PutMapping("/schools/{school_id}/branches/{branch_id}/workspace")
    @RequirePermission(value = "workspace:edit", school = "#schoolId", branch = "#branchId")
    public ApiResponse<WorkspaceResponse> workspace(
            @PathVariable("school_id") UUID schoolId,
            @PathVariable("branch_id") UUID branchId,
            @Valid @RequestBody WorkspaceRequest request) {
        return ApiResponse.success(branches.claimWorkspace(schoolId, branchId, request.getSlug()));
    }

    @GetMapping("/workspaces/{slug}")
    public ApiResponse<WorkspaceResolution> resolve(@PathVariable String slug) {
        return ApiResponse.success(branches.resolve(slug));
    }
}
