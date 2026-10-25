package io.sala.krob_krong.score.service;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.score.dto.request.SaveScoresRequest;
import io.sala.krob_krong.score.dto.response.SaveScoresResponse;
import io.sala.krob_krong.score.dto.response.ScoreResponse;
import io.sala.krob_krong.score.dto.response.StudentScoreResponse;

import java.util.UUID;

public interface ScoreService {

    PageResponse<ScoreResponse> list(UUID schoolId, UUID assessmentId, PageRequest pageRequest);

    SaveScoresResponse save(UUID schoolId, UUID assessmentId, SaveScoresRequest request);

    PageResponse<StudentScoreResponse> listByStudent(UUID schoolId, UUID studentId, PageRequest pageRequest);
}
