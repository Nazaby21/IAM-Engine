package io.sala.krob_krong.iam.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GrantRoleRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID roleId;

    private UUID branchId;

    private Instant validTo;
}
