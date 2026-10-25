package io.sala.krob_krong.score.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SaveScoresRequest {

    @NotEmpty
    @Valid
    private List<ScoreItemRequest> scores;
}
