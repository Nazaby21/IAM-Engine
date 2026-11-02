package io.sala.krob_krong.assessment.service;

import io.sala.krob_krong.assessment.dto.request.CreateAssessmentRequest;
import io.sala.krob_krong.assessment.dto.response.CreateAssessmentResponse;
import io.sala.krob_krong.common.constant.AssessmentType;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import java.util.UUID;

public interface AssessmentService {

    CreateAssessmentResponse create(UUID schoolId, UUID courseId, UUID subjectId, CreateAssessmentRequest request);

    PageResponse<CreateAssessmentResponse> list(
            UUID schoolId, UUID courseId, UUID subjectId, AssessmentType type, PageRequest pageRequest);

    CreateAssessmentResponse getById(UUID schoolId, UUID assessmentId);

    CreateAssessmentResponse updateById(UUID schoolId, UUID assessmentId, CreateAssessmentRequest request);

    void delete(UUID schoolId, UUID assessmentId);
}