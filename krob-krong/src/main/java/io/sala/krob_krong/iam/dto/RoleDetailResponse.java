package io.sala.krob_krong.iam.dto;

import java.util.List;
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
public class RoleDetailResponse {

    private UUID id;
    private String code;
    private String name;
    private String kind;
    private boolean allowsBranch;
    private boolean preApproval;
    private List<RolePermissionResponse> permissions;
    private List<UUID> grantableRoleIds;
}
