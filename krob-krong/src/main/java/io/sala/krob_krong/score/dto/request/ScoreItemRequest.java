package io.sala.krob_krong.score.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ScoreItemRequest {

    @NotNull
    @JsonProperty("student_id")
    private UUID studentId;

    @NotNull
    @Min(0)
    private Integer score;

    private String grade;
}
