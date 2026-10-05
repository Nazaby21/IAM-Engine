package io.sala.krob_krong.iam.specification;

import io.sala.krob_krong.iam.entity.ModuleEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModuleSpecification {

    public static Specification<ModuleEntity> nameLike(String fragment) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(ModuleEntity.Fields.name)), "%" + fragment.toLowerCase() + "%");
    }

    public static Specification<ModuleEntity> sortOrderLessThan(int max) {
        return (root, query, cb) -> cb.lessThan(root.get(ModuleEntity.Fields.sortOrder), max);
    }
}
