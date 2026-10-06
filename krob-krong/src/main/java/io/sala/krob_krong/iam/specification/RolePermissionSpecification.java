package io.sala.krob_krong.iam.specification;

import io.sala.krob_krong.iam.entity.RolePermissionEntity;
import java.util.Collection;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RolePermissionSpecification {

    public static Specification<RolePermissionEntity> byRoleId(UUID roleId) {
        return (root, query, cb) -> cb.equal(root.get(RolePermissionEntity.Fields.roleId), roleId);
    }

    public static Specification<RolePermissionEntity> roleIdIn(Collection<UUID> roleIds) {
        return (root, query, cb) -> root.get(RolePermissionEntity.Fields.roleId).in(roleIds);
    }

    public static Specification<RolePermissionEntity> byPermissionCode(String permissionCode) {
        return (root, query, cb) -> cb.equal(root.get(RolePermissionEntity.Fields.permissionCode), permissionCode);
    }

    public static Specification<RolePermissionEntity> byScope(String scope) {
        return (root, query, cb) -> cb.equal(root.get(RolePermissionEntity.Fields.scope), scope);
    }
}
