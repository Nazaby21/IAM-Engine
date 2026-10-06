package io.sala.krob_krong.branch.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceResponse {
    private UUID id;
    private UUID schoolId;
    private UUID branchId;
    private String slug;
    private Instant createdAt;
    private Instant retiredAt;
}
