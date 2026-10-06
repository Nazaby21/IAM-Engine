package io.sala.krob_krong.school.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolReviewResponse {
    private UUID id;
    private UUID schoolId;
    private UUID submittedBy;
    private Instant submittedAt;
    private Map<String, Object> snapshot;
    private UUID decidedBy;
    private Instant decidedAt;
    private String decision;
    private String notes;
}
