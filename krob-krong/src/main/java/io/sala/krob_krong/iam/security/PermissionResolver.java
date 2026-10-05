package io.sala.krob_krong.iam.security;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface PermissionResolver {

    Set<String> effectivePermissionCodes(UUID userId, UUID schoolId, UUID branchId);

    Set<String> toAuthorities(Collection<String> permissionCodes);
}
