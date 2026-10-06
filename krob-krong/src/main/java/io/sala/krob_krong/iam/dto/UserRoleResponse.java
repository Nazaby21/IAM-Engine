package io.sala.krob_krong.iam.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleResponse {

    private UUID id;
    private UUID userId;
    private UUID roleId;
    private String roleCode;
    private String roleName;
    private UUID schoolId;
    private UUID branchId;
    private String source;
    private Instant validFrom;
    private Instant validTo;
    private UUID grantedBy;
    private Instant grantedAt;
    private Instant acceptedAt;
    private Instant revokedAt;
    private String revokeReason;
    private String status;
}
