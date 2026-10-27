package io.sala.krob_krong.score.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

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
