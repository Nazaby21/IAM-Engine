package io.sala.krob_krong.iam.specification;

import io.sala.krob_krong.iam.entity.RoleEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoleSpecification {

    public static Specification<RoleEntity> byCode(String code) {
        return (root, query, cb) -> cb.equal(root.get(RoleEntity.Fields.code), code);
    }

    public static Specification<RoleEntity> byKind(String kind) {
        return (root, query, cb) -> cb.equal(root.get(RoleEntity.Fields.kind), kind);
    }

    public static Specification<RoleEntity> schoolRoles() {
        return byKind("school");
    }

    public static Specification<RoleEntity> platformRoles() {
        return byKind("platform");
    }

    public static Specification<RoleEntity> allowsBranch() {
        return (root, query, cb) -> cb.isTrue(root.get(RoleEntity.Fields.allowsBranch));
    }

    public static Specification<RoleEntity> preApproval() {
        return (root, query, cb) -> cb.isTrue(root.get(RoleEntity.Fields.preApproval));
    }

    public static Specification<RoleEntity> assignable() {
        return byKind("platform").or(byKind("school"));
    }

    public static Specification<RoleEntity> nameLike(String fragment) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(RoleEntity.Fields.name)), "%" + fragment.toLowerCase() + "%");
    }
}
