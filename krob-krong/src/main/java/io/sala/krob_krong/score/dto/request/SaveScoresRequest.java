package io.sala.krob_krong.score.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SaveScoresRequest {

    @NotEmpty
    @Valid
    private List<ScoreItemRequest> scores;
}
