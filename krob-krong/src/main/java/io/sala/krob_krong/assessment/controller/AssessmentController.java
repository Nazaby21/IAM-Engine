package io.sala.krob_krong.assessment.controller;

import io.sala.krob_krong.assessment.dto.request.CreateAssessmentRequest;
import io.sala.krob_krong.assessment.dto.response.CreateAssessmentResponse;
import io.sala.krob_krong.assessment.service.AssessmentService;
import io.sala.krob_krong.common.constant.AssessmentType;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schools/{school_id}")
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping("/courses/{course_id}/subjects/{subject_id}/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAssessmentResponse create(
            @PathVariable("school_id") UUID schoolId,
            @PathVariable("course_id") UUID courseId,
            @PathVariable("subject_id") UUID subjectId,
            @Valid @RequestBody CreateAssessmentRequest request) {
        return assessmentService.create(schoolId, courseId, subjectId, request);
    }

    @GetMapping("/courses/{course_id}/subjects/{subject_id}/assessments")
    public PageResponse<CreateAssessmentResponse> list(
            @PathVariable("school_id") UUID schoolId,
            @PathVariable("course_id") UUID courseId,
            @PathVariable("subject_id") UUID subjectId,
            @RequestParam(required = false) AssessmentType type,
            @Valid PageRequest pageRequest) {
        return assessmentService.list(schoolId, courseId, subjectId, type, pageRequest);
    }

    @GetMapping("/assessments/{assessment_id}")
    public CreateAssessmentResponse get(
            @PathVariable("school_id") UUID schoolId, @PathVariable("assessment_id") UUID assessmentId) {
        return assessmentService.getById(schoolId, assessmentId);
    }

    @PutMapping("/assessments/{assessment_id}")
    public CreateAssessmentResponse update(
            @PathVariable("school_id") UUID schoolId,
            @PathVariable("assessment_id") UUID assessmentId,
            @Valid @RequestBody CreateAssessmentRequest request) {
        return assessmentService.updateById(schoolId, assessmentId, request);
    }

    @DeleteMapping("/assessments/{assessment_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable("school_id") UUID schoolId,
            @PathVariable("assessment_id") UUID assessmentId) {
        assessmentService.delete(schoolId, assessmentId);
    }
}
