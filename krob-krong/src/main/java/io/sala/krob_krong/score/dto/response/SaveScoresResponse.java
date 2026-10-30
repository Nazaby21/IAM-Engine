package io.sala.krob_krong.score.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SaveScoresResponse {

    private List<ScoreResponse> data;
}
