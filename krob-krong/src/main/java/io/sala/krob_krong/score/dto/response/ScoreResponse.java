package io.sala.krob_krong.score.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ScoreResponse {

    private UUID studentId;
    private Integer score;
    private String grade;
}
