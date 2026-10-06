package io.sala.krob_krong.school.dto;

import jakarta.validation.constraints.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolSummaryResponse {
    private UUID id;
    private String displayName;
    private String status;
    private String statusReason;
}
