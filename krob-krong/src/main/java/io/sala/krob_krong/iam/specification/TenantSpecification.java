package io.sala.krob_krong.iam.specification;

import io.sala.krob_krong.iam.entity.TenantEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TenantSpecification {

    public static Specification<TenantEntity> bySlug(String slug) {
        return (root, query, cb) -> cb.equal(root.get(TenantEntity.Fields.slug), slug);
    }

    public static Specification<TenantEntity> byStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get(TenantEntity.Fields.status), status);
    }

    public static Specification<TenantEntity> nameLike(String fragment) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(TenantEntity.Fields.name)), "%" + fragment.toLowerCase() + "%");
    }
}
