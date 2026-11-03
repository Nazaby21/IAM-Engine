package io.sala.krob_krong.score.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StudentScoreResponse {

    private UUID studentId;
    private Integer score;
    private String grade;
    private UUID assessmentId;
    private String assessment;
}
