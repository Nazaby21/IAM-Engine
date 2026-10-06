package io.sala.krob_krong.branch.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponse {
    private UUID id;
    private UUID schoolId;
    private String name;
    private boolean defaultBranch;
    private String status;
    private String addressLine;
    private String district;
    private String province;
    private String countryCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private String workspaceSlug;
    private Instant openedAt;
    private Instant createdAt;
}
