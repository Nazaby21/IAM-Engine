package io.sala.krob_krong.course.controller;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.course.dto.CourseCreateRequest;
import io.sala.krob_krong.course.dto.CourseResponse;
import io.sala.krob_krong.course.dto.CourseUpdateRequest;
import io.sala.krob_krong.course.service.CourseService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @GetMapping("/branch/{branch_id}/courses")
    // @RequirePermission(value = "branch:read", school = "#branchId")
    public ApiResponse<PageResponse<CourseResponse>> list(@PathVariable("branch_id") UUID branchId, PageRequest page) {
        return ApiResponse.success(courseService.listAllCourseInBranch(branchId, page));
    }

    @GetMapping("/branch/{branchId}/courses/{courseId}")
    public ApiResponse<CourseResponse> getById(
            @PathVariable("branchId") UUID branchId, @PathVariable("courseId") UUID courseId) {
        return ApiResponse.success(courseService.getById(branchId, courseId));
    }

    @PostMapping("/branch/{branchId}/courses")
    public ApiResponse<CourseResponse> create(
            @PathVariable("branchId") UUID branchId, @Valid @RequestBody CourseCreateRequest request) {
        return ApiResponse.success(courseService.create(branchId, request));
    }

    @PostMapping("/branch/{branchId}/courses/{courseId}/update")
    public ApiResponse<CourseResponse> update(
            @PathVariable("branchId") UUID branchId,
            @PathVariable("courseId") UUID courseId,
            @Valid @RequestBody CourseUpdateRequest request) {
        return ApiResponse.success(courseService.update(branchId, courseId, request));
    }

    @PostMapping("/branch/{branchId}/courses/{courseId}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("branchId") UUID branchId, @PathVariable("courseId") UUID courseId) {
        courseService.delete(branchId, courseId);
    }
}
