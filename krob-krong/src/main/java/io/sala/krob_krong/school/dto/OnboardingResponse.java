package io.sala.krob_krong.school.dto;

import io.sala.krob_krong.branch.dto.BranchResponse;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingResponse {
    private SchoolResponse school;
    private BranchResponse defaultBranch;
    private List<String> missingFields;
    private boolean canSubmit;
}
