package io.sala.krob_krong.iam.security.policy;

import io.sala.krob_krong.iam.entity.PermissionEntity;
import io.sala.krob_krong.iam.entity.RoleEntity;
import io.sala.krob_krong.iam.entity.RoleGrantRuleEntity;
import io.sala.krob_krong.iam.entity.RolePermissionEntity;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RoleCatalog {

    public static final String KIND_ANYONE = "anyone";
    public static final String KIND_AUTHENTICATED = "authenticated";
    public static final String KIND_PLATFORM = "platform";
    public static final String KIND_SCHOOL = "school";
    public static final String KIND_SUPPORT = "support";

    private final Map<UUID, String> kindByRole;
    private final Set<UUID> branchScopedRoles;
    private final Map<UUID, Map<String, Set<String>>> scopesByRole;
    private final Map<UUID, Set<UUID>> granteesByGranter;
    private final List<String> permissionCodes;
    private final Set<String> sensitivePermissions;

    private RoleCatalog(
            Map<UUID, String> kindByRole,
            Set<UUID> branchScopedRoles,
            Map<UUID, Map<String, Set<String>>> scopesByRole,
            Map<UUID, Set<UUID>> granteesByGranter,
            List<String> permissionCodes,
            Set<String> sensitivePermissions) {
        this.kindByRole = kindByRole;
        this.branchScopedRoles = branchScopedRoles;
        this.scopesByRole = scopesByRole;
        this.granteesByGranter = granteesByGranter;
        this.permissionCodes = permissionCodes;
        this.sensitivePermissions = sensitivePermissions;
    }

    public static RoleCatalog of(
            Collection<RoleEntity> roles,
            Collection<RolePermissionEntity> rolePermissions,
            Collection<RoleGrantRuleEntity> grantRules,
            Collection<PermissionEntity> permissions) {
        Map<UUID, String> kindByRole = new LinkedHashMap<>();
        Set<UUID> branchScopedRoles = new HashSet<>();
        for (RoleEntity role : roles) {
            kindByRole.put(role.getId(), role.getKind());
            if (role.isAllowsBranch()) {
                branchScopedRoles.add(role.getId());
            }
        }

        Map<UUID, Map<String, Set<String>>> scopesByRole = new HashMap<>();
        for (RolePermissionEntity rolePermission : rolePermissions) {
            scopesByRole
                    .computeIfAbsent(rolePermission.getRoleId(), roleId -> new HashMap<>())
                    .computeIfAbsent(rolePermission.getPermissionCode(), code -> new HashSet<>())
                    .add(rolePermission.getScope());
        }

        Map<UUID, Set<UUID>> granteesByGranter = new HashMap<>();
        for (RoleGrantRuleEntity rule : grantRules) {
            granteesByGranter
                    .computeIfAbsent(rule.getGranterRoleId(), roleId -> new HashSet<>())
                    .add(rule.getGranteeRoleId());
        }

        List<String> permissionCodes =
                permissions.stream().map(PermissionEntity::getCode).sorted().toList();
        Set<String> sensitivePermissions = permissions.stream()
                .filter(PermissionEntity::isSensitive)
                .map(PermissionEntity::getCode)
                .collect(Collectors.toUnmodifiableSet());

        return new RoleCatalog(
                Collections.unmodifiableMap(kindByRole),
                Collections.unmodifiableSet(branchScopedRoles),
                scopesByRole,
                granteesByGranter,
                permissionCodes,
                sensitivePermissions);
    }

    public Set<UUID> roleIds() {
        return kindByRole.keySet();
    }

    public String kindOf(UUID roleId) {
        return kindByRole.get(roleId);
    }

    public List<UUID> rolesOfKind(String kind) {
        return kindByRole.entrySet().stream()
                .filter(entry -> kind.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public boolean allowsBranch(UUID roleId) {
        return branchScopedRoles.contains(roleId);
    }

    public Set<String> scopes(UUID roleId, String permission) {
        return Collections.unmodifiableSet(
                scopesByRole.getOrDefault(roleId, Map.of()).getOrDefault(permission, Set.of()));
    }

    public boolean grants(UUID roleId, String permission) {
        return !scopes(roleId, permission).isEmpty();
    }

    public boolean canGrant(UUID granterRoleId, UUID granteeRoleId) {
        return granteesByGranter.getOrDefault(granterRoleId, Set.of()).contains(granteeRoleId);
    }

    public List<String> permissionCodes() {
        return permissionCodes;
    }

    public boolean isSensitive(String permission) {
        return sensitivePermissions.contains(permission);
    }
}
