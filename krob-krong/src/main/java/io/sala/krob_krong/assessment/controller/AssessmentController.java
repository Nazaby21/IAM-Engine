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
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/schools/{schoolId}")
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping("/courses/{courseId}/subjects/{subjectId}/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('assessment.create')")
    public CreateAssessmentResponse create(
            @PathVariable UUID schoolId,
            @PathVariable UUID courseId,
            @PathVariable UUID subjectId,
            @Valid @RequestBody CreateAssessmentRequest request) {
        return assessmentService.create(schoolId, courseId, subjectId, request);
    }

    @GetMapping("/courses/{courseId}/subjects/{subjectId}/assessments")
    @PreAuthorize("hasAuthority('assessment.read')")
    public PageResponse<CreateAssessmentResponse> list(
            @PathVariable UUID schoolId,
            @PathVariable UUID courseId,
            @PathVariable UUID subjectId,
            @RequestParam(required = false) AssessmentType type,
            @Valid PageRequest pageRequest) {
        return assessmentService.list(schoolId, courseId, subjectId, type, pageRequest);
    }

    @GetMapping("/assessments/{assessmentId}")
    @PreAuthorize("hasAuthority('assessment.read')")
    public CreateAssessmentResponse get(@PathVariable UUID schoolId, @PathVariable UUID assessmentId) {
        return assessmentService.getById(schoolId, assessmentId);
    }

    @PutMapping("/assessments/{assessmentId}")
    @PreAuthorize("hasAuthority('assessment.update')")
    public CreateAssessmentResponse update(
            @PathVariable UUID schoolId,
            @PathVariable UUID assessmentId,
            @Valid @RequestBody CreateAssessmentRequest request) {
        return assessmentService.updateById(schoolId, assessmentId, request);
    }

    @DeleteMapping("/assessments/{assessmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('assessment.delete')")
    public void Delete(@PathVariable UUID schoolId, @PathVariable UUID assessmentId) {
        assessmentService.delete(schoolId, assessmentId);
    }
}