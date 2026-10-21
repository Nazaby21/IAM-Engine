package io.sala.krob_krong.subject.specification;

import io.sala.krob_krong.subject.entity.SubjectEntity;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SubjectSpecification {

    public static Specification<SubjectEntity> byBranchId(UUID branchId) {
        return (root, query, cb) -> cb.equal(root.get(SubjectEntity.Fields.branchId), branchId);
    }

    public static Specification<SubjectEntity> byStatus(String status) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get(SubjectEntity.Fields.status)), status.toLowerCase(Locale.ROOT));
    }

    public static Specification<SubjectEntity> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get(SubjectEntity.Fields.deletedAt));
    }
}
