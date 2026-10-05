package io.sala.krob_krong.iam.config.security;

import io.sala.krob_krong.iam.entity.RoleEntity;
import java.util.Collection;
import java.util.Set;

public interface PermissionResolver {

    Set<String> effectivePermissionCodes(Collection<RoleEntity> roles);

    Set<String> toAuthorities(Collection<String> permissionCodes);

    int highestRank(Collection<RoleEntity> roles);
}
