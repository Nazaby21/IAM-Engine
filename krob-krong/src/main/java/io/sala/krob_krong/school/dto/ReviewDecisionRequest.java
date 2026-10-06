package io.sala.krob_krong.school.dto;

import jakarta.validation.constraints.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDecisionRequest {
    @NotNull
    private UUID reviewId;

    @NotBlank
    @Pattern(regexp = "approved|changes_requested|rejected")
    private String decision;

    @Size(max = 2000)
    private String reason;
}
