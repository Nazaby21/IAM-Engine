package io.sala.krob_krong.score.controller;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.score.dto.request.SaveScoresRequest;
import io.sala.krob_krong.score.dto.response.SaveScoresResponse;
import io.sala.krob_krong.score.dto.response.ScoreResponse;
import io.sala.krob_krong.score.dto.response.StudentScoreResponse;
import io.sala.krob_krong.score.service.ScoreService;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/schools/{schoolId}")
public class ScoreController {

    private final ScoreService scoreService;

    @GetMapping("/assessments/{assessmentId}/scores")
    @PreAuthorize("hasAuthority('score.read')")
    public PageResponse<ScoreResponse> list(
            @PathVariable UUID schoolId, @PathVariable UUID assessmentId, PageRequest pageRequest) {
        return scoreService.list(schoolId, assessmentId, pageRequest);
    }

    @PutMapping("/assessments/{assessmentId}/scores")
    @PreAuthorize("hasAuthority('score.record')")
    public SaveScoresResponse save(
            @PathVariable UUID schoolId,
            @PathVariable UUID assessmentId,
            @Valid @RequestBody SaveScoresRequest request) {
        return scoreService.save(schoolId, assessmentId, request);
    }

    @GetMapping("/students/{studentId}/scores")
    @PreAuthorize("hasAuthority('score.read')")
    public PageResponse<StudentScoreResponse> listByStudent(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @Valid PageRequest pageRequest) {
        return scoreService.listByStudent(schoolId, studentId, pageRequest);
    }
}
