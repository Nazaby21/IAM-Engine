package io.sala.krob_krong.iam.security.policy;

import io.sala.krob_krong.iam.entity.PermissionEntity;
import io.sala.krob_krong.iam.entity.RoleEntity;
import io.sala.krob_krong.iam.entity.RoleGrantRuleEntity;
import io.sala.krob_krong.iam.entity.RolePermissionEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A subset of V1_0_10__SEED_DATA.sql, kept to the same kinds, scopes and grant rules. */
public final class SeedRoles {

    public static final UUID VISITOR = UUID.randomUUID();
    public static final UUID MEMBER = UUID.randomUUID();
    public static final UUID SUPER_ADMIN = UUID.randomUUID();
    public static final UUID SUPPORT_VIEWER = UUID.randomUUID();
    public static final UUID SCHOOL_OWNER = UUID.randomUUID();
    public static final UUID SCHOOL_ADMIN = UUID.randomUUID();
    public static final UUID INSTRUCTOR = UUID.randomUUID();

    private static final Set<String> SENSITIVE = Set.of("member:read", "support:start");

    private SeedRoles() {}

    public static RoleCatalog catalog() {
        List<RoleEntity> roles = List.of(
                role(VISITOR, "anyone", false),
                role(MEMBER, "authenticated", false),
                role(SUPER_ADMIN, "platform", false),
                role(SUPPORT_VIEWER, "support", false),
                role(SCHOOL_OWNER, "school", false),
                role(SCHOOL_ADMIN, "school", true),
                role(INSTRUCTOR, "school", true));

        List<RolePermissionEntity> rolePermissions = new ArrayList<>();
        scope(rolePermissions, VISITOR, "public", "school.profile:read", "branch:read", "media.public:view");
        scope(rolePermissions, MEMBER, "own", "member:read");
        scope(rolePermissions, SUPER_ADMIN, "platform", "school:review", "support:start");
        scope(rolePermissions, SUPPORT_VIEWER, "tenant", "school.profile:read", "member:read");
        scope(
                rolePermissions,
                SCHOOL_OWNER,
                "tenant",
                "school.profile:read",
                "school.profile:edit",
                "branch:read",
                "branch:edit",
                "member:read",
                "media:upload");
        scope(
                rolePermissions,
                SCHOOL_ADMIN,
                "tenant",
                "school.profile:read",
                "branch:read",
                "branch:edit",
                "member:read",
                "media:upload");
        scope(rolePermissions, INSTRUCTOR, "tenant", "school.profile:read", "branch:read", "media:upload");

        List<RoleGrantRuleEntity> grantRules = List.of(
                rule(SUPER_ADMIN, SUPER_ADMIN),
                rule(SUPER_ADMIN, SCHOOL_OWNER),
                rule(SCHOOL_OWNER, SCHOOL_OWNER),
                rule(SCHOOL_OWNER, SCHOOL_ADMIN),
                rule(SCHOOL_OWNER, INSTRUCTOR),
                rule(SCHOOL_ADMIN, INSTRUCTOR));

        List<PermissionEntity> permissions = rolePermissions.stream()
                .map(RolePermissionEntity::getPermissionCode)
                .distinct()
                .map(SeedRoles::permission)
                .toList();

        return RoleCatalog.of(roles, rolePermissions, grantRules, permissions);
    }

    private static RoleEntity role(UUID id, String kind, boolean allowsBranch) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setCode(kind + "-" + id);
        role.setName(kind);
        role.setKind(kind);
        role.setAllowsBranch(allowsBranch);
        return role;
    }

    private static void scope(List<RolePermissionEntity> target, UUID roleId, String scope, String... codes) {
        for (String code : codes) {
            RolePermissionEntity rolePermission = new RolePermissionEntity();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionCode(code);
            rolePermission.setScope(scope);
            target.add(rolePermission);
        }
    }

    private static RoleGrantRuleEntity rule(UUID granter, UUID grantee) {
        RoleGrantRuleEntity rule = new RoleGrantRuleEntity();
        rule.setGranterRoleId(granter);
        rule.setGranteeRoleId(grantee);
        return rule;
    }

    private static PermissionEntity permission(String code) {
        PermissionEntity permission = new PermissionEntity();
        permission.setCode(code);
        permission.setDescription(code);
        permission.setSensitive(SENSITIVE.contains(code));
        return permission;
    }
}
