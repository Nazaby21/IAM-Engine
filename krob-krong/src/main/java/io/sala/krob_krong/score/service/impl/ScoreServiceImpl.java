package io.sala.krob_krong.score.service.impl;

import io.sala.krob_krong.assessment.entity.AssessmentEntity;
import io.sala.krob_krong.assessment.repository.AssessmentRepository;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.score.dto.request.SaveScoresRequest;
import io.sala.krob_krong.score.dto.request.ScoreItemRequest;
import io.sala.krob_krong.score.dto.response.SaveScoresResponse;
import io.sala.krob_krong.score.dto.response.ScoreResponse;
import io.sala.krob_krong.score.dto.response.StudentScoreResponse;
import io.sala.krob_krong.score.entity.ScoreEntity;
import io.sala.krob_krong.score.repository.ScoreRepository;
import io.sala.krob_krong.score.service.ScoreService;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private final ScoreRepository scoreRepository;
    private final AssessmentRepository assessmentRepository;

    @Override
    public PageResponse<ScoreResponse> list(UUID schoolId, UUID assessmentId, PageRequest pageRequest) {
        if (!assessmentRepository.existsInSchool(assessmentId, schoolId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found");
        }

        Page<ScoreResponse> result = scoreRepository
                .findByAssessmentId(assessmentId, pageRequest.toSpring())
                .map(s -> ScoreResponse.builder()
                        .studentId(s.getStudentId())
                        .score(s.getScore())
                        .grade(s.getGrade())
                        .build());

        return PageResponse.from(result);
    }

    @Override
    public SaveScoresResponse save(UUID schoolId, UUID assessmentId, SaveScoresRequest request) {
        AssessmentEntity assessment = assessmentRepository
                .findInSchool(assessmentId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));

        List<UUID> studentIds =
                request.getScores().stream().map(ScoreItemRequest::getStudentId).toList();

        if (new HashSet<>(studentIds).size() != studentIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate student_id in request");
        }

        boolean overTotal = request.getScores().stream()
                .anyMatch(i -> BigDecimal.valueOf(i.getScore()).compareTo(assessment.getTotalScore()) > 0);
        if (overTotal) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "score must not exceed total_score");
        }

        Set<UUID> enrolled =
                scoreRepository.findEnrolledStudentIds(assessment.getCourseId(), assessment.getSubjectId(), studentIds);
        if (!enrolled.containsAll(studentIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student is not enrolled in this subject");
        }

        Map<UUID, ScoreEntity> existing =
                scoreRepository.findByAssessmentIdAndStudentIdIn(assessmentId, studentIds).stream()
                        .collect(Collectors.toMap(ScoreEntity::getStudentId, Function.identity()));

        List<ScoreEntity> toSave = request.getScores().stream()
                .map(i -> {
                    ScoreEntity e = existing.getOrDefault(i.getStudentId(), new ScoreEntity());
                    e.setAssessmentId(assessmentId);
                    e.setStudentId(i.getStudentId());
                    e.setScore(i.getScore());
                    e.setGrade(i.getGrade());
                    return e;
                })
                .toList();

        return SaveScoresResponse.builder()
                .data(scoreRepository.saveAll(toSave).stream()
                        .map(s -> ScoreResponse.builder()
                                .studentId(s.getStudentId())
                                .score(s.getScore())
                                .grade(s.getGrade())
                                .build())
                        .toList())
                .build();
    }

    @Override
    public PageResponse<StudentScoreResponse> listByStudent(UUID schoolId, UUID studentId, PageRequest pageRequest) {
        if (!assessmentRepository.existsInSchool(studentId, schoolId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found");
        }
        return PageResponse.from(scoreRepository.findByStudentId(studentId, pageRequest.toSpring()));
    }
}
