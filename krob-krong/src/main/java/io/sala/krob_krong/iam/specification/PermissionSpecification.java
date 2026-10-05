package io.sala.krob_krong.iam.specification;

import io.sala.krob_krong.iam.entity.PermissionEntity;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PermissionSpecification {

    public static Specification<PermissionEntity> byModuleCode(String moduleCode) {
        return (root, query, cb) -> cb.equal(root.get(PermissionEntity.Fields.moduleCode), moduleCode);
    }

    public static Specification<PermissionEntity> byAction(String action) {
        return (root, query, cb) -> cb.equal(root.get(PermissionEntity.Fields.action), action);
    }

    public static Specification<PermissionEntity> codeIn(Collection<String> codes) {
        return (root, query, cb) -> root.get(PermissionEntity.Fields.code).in(codes);
    }
}
