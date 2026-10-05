package io.sala.krob_krong.iam.specification;

import io.sala.krob_krong.iam.entity.RoleEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoleSpecification {

    public static Specification<RoleEntity> byTenantId(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get(RoleEntity.Fields.tenantId), tenantId);
    }

    public static Specification<RoleEntity> globalOnly() {
        return (root, query, cb) -> cb.isNull(root.get(RoleEntity.Fields.tenantId));
    }

    public static Specification<RoleEntity> byTenantIdOrGlobal(String tenantId) {
        return byTenantId(tenantId).or(globalOnly());
    }

    public static Specification<RoleEntity> byRoleKey(String roleKey) {
        return (root, query, cb) -> cb.equal(root.get(RoleEntity.Fields.roleKey), roleKey);
    }

    public static Specification<RoleEntity> isSystem() {
        return (root, query, cb) -> cb.isTrue(root.get(RoleEntity.Fields.system));
    }

    public static Specification<RoleEntity> isDefault() {
        return (root, query, cb) -> cb.isTrue(root.get(RoleEntity.Fields.defaultRole));
    }

    public static Specification<RoleEntity> byParentRoleId(String parentRoleId) {
        return (root, query, cb) -> cb.equal(root.get(RoleEntity.Fields.parentRoleId), parentRoleId);
    }

    public static Specification<RoleEntity> rankAtLeast(int minRank) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(RoleEntity.Fields.rank), minRank);
    }

    public static Specification<RoleEntity> nameLike(String fragment) {
        return (root, query, cb) -> cb.like(cb.lower(root.get(RoleEntity.Fields.name)), "%" + fragment.toLowerCase() + "%");
    }
}
