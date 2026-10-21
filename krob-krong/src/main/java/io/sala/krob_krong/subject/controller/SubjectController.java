package io.sala.krob_krong.subject.controller;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.subject.dto.SubjectCreateRequest;
import io.sala.krob_krong.subject.dto.SubjectResponse;
import io.sala.krob_krong.subject.dto.SubjectUpdateRequest;
import io.sala.krob_krong.subject.service.SubjectService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    @GetMapping("/branch/{branch_id}/subjects")
    // @RequirePermission(value = "branch:read", school = "#branchId")
    public ApiResponse<PageResponse<SubjectResponse>> list(@PathVariable("branch_id") UUID branchId, PageRequest page) {
        return ApiResponse.success(subjectService.getAllSubjectInBranch(branchId, page));
    }

    @GetMapping("/branch/{branch_id}/subjects/{subject_id}")
    public ApiResponse<SubjectResponse> getById(
            @PathVariable("branch_id") UUID branchId, @PathVariable("subject_id") UUID subjectId) {
        return ApiResponse.success(subjectService.getById(branchId, subjectId));
    }

    @PostMapping("/branch/{branch_id}/subjects")
    // @RequirePermission(value = "branch:create", school = "#schoolId")
    public ApiResponse<SubjectResponse> create(
            @PathVariable("branch_id") UUID branchId, @Valid @RequestBody SubjectCreateRequest request) {
        return ApiResponse.create(subjectService.create(branchId, request));
    }

    @PostMapping("/branch/{branchId}/subjects/{subjectId}/update")
    // @RequirePermission(value = "branch:edit", school = "#branchId")
    public ApiResponse<SubjectResponse> update(
            @PathVariable("branchId") UUID branchId,
            @PathVariable("subjectId") UUID subjectId,
            @Valid @RequestBody SubjectUpdateRequest request) {
        return ApiResponse.success(subjectService.update(branchId, subjectId, request));
    }

    @PostMapping("/branch/{branchId}/subjects/{subjectId}/delete")
    // @RequirePermission(value = "branch:edit", school = "#branchId")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID branchId, @PathVariable UUID subjectId) {
        subjectService.delete(branchId, subjectId);
    }
}
