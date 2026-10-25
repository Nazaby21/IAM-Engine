package io.sala.krob_krong.assessment.service.impl;

import io.sala.krob_krong.assessment.dto.request.CreateAssessmentRequest;
import io.sala.krob_krong.assessment.dto.response.CreateAssessmentResponse;
import io.sala.krob_krong.assessment.entity.AssessmentEntity;
import io.sala.krob_krong.assessment.repository.AssessmentRepository;
import io.sala.krob_krong.assessment.service.AssessmentService;
import io.sala.krob_krong.common.constant.AssessmentStatus;
import io.sala.krob_krong.common.constant.AssessmentType;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.course_subject.repository.CourseSubjectRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Phnom_Penh");

    private final AssessmentRepository assessmentRepository;
    private final CourseSubjectRepository courseSubjectRepository;

    @Override
    @Transactional
    public CreateAssessmentResponse create(
            UUID schoolId, UUID courseId, UUID subjectId, CreateAssessmentRequest request) {
        checkCourseSubject(schoolId, courseId, subjectId);

        LocalDate start = LocalDate.parse(request.getStartDate());
        LocalDate end = LocalDate.parse(request.getEndDate());
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end_date must not be before start_date");
        }

        AssessmentEntity entity = new AssessmentEntity();
        entity.setCourseId(courseId);
        entity.setSubjectId(subjectId);
        entity.setType(request.getAssessmentType());
        entity.setDescription(request.getDescription());
        entity.setStartedDate(start.atStartOfDay(ZONE).toInstant());
        entity.setEndedDate(end.atStartOfDay(ZONE).toInstant());
        entity.setDuration(request.getDuration());
        entity.setTotalScore(BigDecimal.valueOf(request.getTotalScore()));
        entity.setStatus(AssessmentStatus.ACTIVE);

        return toResponse(assessmentRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CreateAssessmentResponse> list(
            UUID schoolId, UUID courseId, UUID subjectId, AssessmentType type, PageRequest pageRequest) {
        checkCourseSubject(schoolId, courseId, subjectId);

        Pageable pageable = pageRequest.toSpring();
        Page<AssessmentEntity> result = type == null
                ? assessmentRepository.findByCourseIdAndSubjectId(courseId, subjectId, pageable)
                : assessmentRepository.findByCourseIdAndSubjectIdAndType(courseId, subjectId, type, pageable);

        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    public CreateAssessmentResponse getById(UUID schoolId, UUID assessmentId) {
        return assessmentRepository
                .findInSchool(assessmentId, schoolId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
    }

    @Override
    public CreateAssessmentResponse updateById(UUID schoolId, UUID assessmentId, CreateAssessmentRequest request) {
        AssessmentEntity entity = assessmentRepository
                .findById(assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
        LocalDate start = LocalDate.parse(request.getStartDate());
        LocalDate end = LocalDate.parse(request.getEndDate());
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end_date must not be before start_date");
        }
        entity.setType(request.getAssessmentType());
        entity.setDescription(request.getDescription());
        entity.setStartedDate(start.atStartOfDay(ZONE).toInstant());
        entity.setEndedDate(end.atStartOfDay(ZONE).toInstant());
        entity.setDuration(request.getDuration());
        entity.setTotalScore(BigDecimal.valueOf(request.getTotalScore()));

        return toResponse(assessmentRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID schoolId, UUID assessmentId) {
        AssessmentEntity entity = assessmentRepository
                .findInSchool(assessmentId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));

        if (assessmentRepository.hasScores(assessmentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Assessment already has scores");
        }

        assessmentRepository.delete(entity);
    }

    private void checkCourseSubject(UUID schoolId, UUID courseId, UUID subjectId) {
        if (!courseSubjectRepository.existsInSchool(courseId, subjectId, schoolId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course subject not found");
        }
    }

    private CreateAssessmentResponse toResponse(AssessmentEntity a) {
        return CreateAssessmentResponse.builder()
                .id(a.getId())
                .courseSubjectId(a.getCourseId())
                .type(a.getType())
                .description(a.getDescription())
                .startDate(a.getStartedDate().atZone(ZONE).toLocalDate())
                .endDate(a.getEndedDate().atZone(ZONE).toLocalDate())
                .duration(a.getDuration())
                .totalScore(a.getTotalScore().intValue())
                .status(a.getStatus())
                .build();
    }
}
