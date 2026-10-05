package io.sala.krob_krong.iam.security.impl;

import io.sala.krob_krong.iam.security.PermissionResolver;
import io.sala.krob_krong.iam.rbac.entity.RolePermissionEntity;
import io.sala.krob_krong.iam.rbac.entity.UserRoleEntity;
import io.sala.krob_krong.iam.rbac.repository.RolePermissionRepository;
import io.sala.krob_krong.iam.rbac.repository.UserRoleRepository;
import io.sala.krob_krong.iam.rbac.specification.RolePermissionSpecification;
import io.sala.krob_krong.iam.rbac.specification.UserRoleSpecification;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionResolverImpl implements PermissionResolver {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public Set<String> effectivePermissionCodes(UUID userId, UUID schoolId, UUID branchId) {
        List<UserRoleEntity> grants = userRoleRepository.findAll(
                UserRoleSpecification.byUserId(userId)
                        .and(UserRoleSpecification.activeNow())
                        .and(schoolId != null
                                ? UserRoleSpecification.bySchoolId(schoolId)
                                : UserRoleSpecification.platformGrants()));

        if (grants.isEmpty()) {
            return Set.of();
        }

        Set<UUID> roleIds = grants.stream().map(UserRoleEntity::getRoleId).collect(Collectors.toSet());

        List<RolePermissionEntity> rolePerms =
                rolePermissionRepository.findAll(RolePermissionSpecification.roleIdIn(roleIds));

        Set<String> codes = new LinkedHashSet<>();
        for (RolePermissionEntity rp : rolePerms) {
            codes.add(rp.getPermissionCode());
        }
        return codes;
    }

    @Override
    public Set<String> toAuthorities(Collection<String> permissionCodes) {
        Set<String> authorities = new LinkedHashSet<>();
        for (String code : permissionCodes) {
            authorities.add(code + ":*");
        }
        return authorities;
    }
}
