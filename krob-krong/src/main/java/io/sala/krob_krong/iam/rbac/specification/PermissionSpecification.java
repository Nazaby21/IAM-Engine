package io.sala.krob_krong.iam.rbac.specification;

import io.sala.krob_krong.iam.rbac.entity.PermissionEntity;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PermissionSpecification {

    public static Specification<PermissionEntity> byCode(String code) {
        return (root, query, cb) -> cb.equal(root.get(PermissionEntity.Fields.code), code);
    }

    public static Specification<PermissionEntity> codeIn(Collection<String> codes) {
        return (root, query, cb) -> root.get(PermissionEntity.Fields.code).in(codes);
    }

    public static Specification<PermissionEntity> codeLike(String prefix) {
        return (root, query, cb) -> cb.like(root.get(PermissionEntity.Fields.code), prefix + "%");
    }

    public static Specification<PermissionEntity> isSensitive() {
        return (root, query, cb) -> cb.isTrue(root.get(PermissionEntity.Fields.sensitive));
    }
}
