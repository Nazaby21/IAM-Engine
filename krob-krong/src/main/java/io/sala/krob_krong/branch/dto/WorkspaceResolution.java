package io.sala.krob_krong.branch.dto;

import jakarta.validation.constraints.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceResolution {
    private UUID schoolId;
    private UUID branchId;
    private String canonicalSlug;
}
