package io.sala.krob_krong.school.specification;

import io.sala.krob_krong.school.entity.SchoolEntity;
import java.util.Collection;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SchoolSpecification {

    public static Specification<SchoolEntity> byStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get(SchoolEntity.Fields.status), status);
    }

    public static Specification<SchoolEntity> statusIn(Collection<String> statuses) {
        return (root, query, cb) -> root.get(SchoolEntity.Fields.status).in(statuses);
    }

    public static Specification<SchoolEntity> byCreatedBy(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get(SchoolEntity.Fields.createdBy), userId);
    }

    public static Specification<SchoolEntity> displayNameLike(String fragment) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(SchoolEntity.Fields.displayName)), "%" + fragment.toLowerCase() + "%");
    }

    public static Specification<SchoolEntity> pendingReview() {
        return byStatus("pending_review");
    }

    public static Specification<SchoolEntity> approved() {
        return byStatus("approved");
    }

    public static Specification<SchoolEntity> suspended() {
        return byStatus("suspended");
    }
}
