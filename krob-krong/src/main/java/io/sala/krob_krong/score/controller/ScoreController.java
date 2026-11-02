package io.sala.krob_krong.score.controller;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.score.dto.request.SaveScoresRequest;
import io.sala.krob_krong.score.dto.response.SaveScoresResponse;
import io.sala.krob_krong.score.dto.response.ScoreResponse;
import io.sala.krob_krong.score.dto.response.StudentScoreResponse;
import io.sala.krob_krong.score.service.ScoreService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schools/{school_id}")
public class ScoreController {

    private final ScoreService scoreService;

    @GetMapping("/assessments/{assessment_id}/scores")
    public PageResponse<ScoreResponse> list(
            @PathVariable("school_id") UUID schoolId,
            @PathVariable("assessment_id") UUID assessmentId,
            PageRequest pageRequest) {
        return scoreService.list(schoolId, assessmentId, pageRequest);
    }

    @PutMapping("/assessments/{assessment_id}/scores")
    public SaveScoresResponse save(
            @PathVariable("school_id") UUID schoolId,
            @PathVariable("assessment_id") UUID assessmentId,
            @Valid @RequestBody SaveScoresRequest request) {
        return scoreService.save(schoolId, assessmentId, request);
    }

    @GetMapping("/students/{student_id}/scores")
    public PageResponse<StudentScoreResponse> listByStudent(
            @PathVariable("school_id") UUID schoolId,
            @PathVariable("student_id") UUID studentId,
            @Valid PageRequest pageRequest) {
        return scoreService.listByStudent(schoolId, studentId, pageRequest);
    }
}
