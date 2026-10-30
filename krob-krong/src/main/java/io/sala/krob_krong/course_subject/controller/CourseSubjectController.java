package io.sala.krob_krong.course_subject.controller;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.course_subject.dto.request.CourseSubjectCreateRequest;
import io.sala.krob_krong.course_subject.dto.request.CourseSubjectUpdateRequest;
import io.sala.krob_krong.course_subject.dto.response.CourseSubjectResponse;
import io.sala.krob_krong.course_subject.service.CourseSubjectService;
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
@RequestMapping("/api/course-subject")
@RequiredArgsConstructor
public class CourseSubjectController {
    private final CourseSubjectService courseSubjectService;

    @GetMapping("/{course_id}")
    public ApiResponse<PageResponse<CourseSubjectResponse>> list(
            @PathVariable("course_id") UUID courseId, PageRequest pageRequest) {
        return ApiResponse.success(courseSubjectService.listAllSubjectInCourse(courseId, pageRequest));
    }

    @PostMapping("/{courseId}/subjects")
    public ApiResponse<CourseSubjectResponse> create(
            @PathVariable("courseId") UUID courseId, @Valid @RequestBody CourseSubjectCreateRequest request) {
        return ApiResponse.success(courseSubjectService.create(courseId, request));
    }

    @PostMapping("/{courseId}/subjects/{subjectId}/update")
    public ApiResponse<CourseSubjectResponse> update(
            @PathVariable("courseId") UUID courseId,
            @PathVariable("subjectId") UUID subjectId,
            @Valid @RequestBody CourseSubjectUpdateRequest request) {
        return ApiResponse.success(courseSubjectService.update(courseId, subjectId, request));
    }

    @PostMapping("/{courseId}/subjects/{subjectId}/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable("courseId") UUID courseId, @PathVariable("subjectId") UUID subjectId) {
        courseSubjectService.remove(courseId, subjectId);
    }
}