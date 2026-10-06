package io.sala.krob_krong.school.dto;

import jakarta.validation.constraints.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusReasonRequest {
    @NotBlank
    @Size(max = 2000)
    private String reason;
}
