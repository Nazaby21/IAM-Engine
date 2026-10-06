package io.sala.krob_krong.iam.security;

import java.util.Set;
import java.util.UUID;

public interface AccessPolicyService {

    boolean canAccess(UUID userId, String permission, UUID schoolId, UUID branchId, UUID targetUserId);

    boolean canGrantRole(UUID granterId, UUID roleId, UUID schoolId, UUID branchId);

    Set<String> effectivePermissions(UUID userId, UUID schoolId, UUID branchId);

    Set<UUID> grantableRoleIds(UUID granterId, UUID schoolId, UUID branchId);

    boolean isSensitive(String permission);
}
