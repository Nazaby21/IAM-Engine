package io.sala.krob_krong.score.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class SubjectScoreResponse {
    private UUID subjectId;
    private Long totalScore;
    private BigDecimal maxScore;
}
