package io.sala.krob_krong.branch.specification;

import io.sala.krob_krong.branch.entity.WorkspaceEntity;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WorkspaceSpecification {

    public static Specification<WorkspaceEntity> bySlug(String slug) {
        return (root, query, cb) -> cb.equal(root.get(WorkspaceEntity.Fields.slug), slug.toLowerCase());
    }

    public static Specification<WorkspaceEntity> bySchoolId(UUID schoolId) {
        return (root, query, cb) -> cb.equal(root.get(WorkspaceEntity.Fields.schoolId), schoolId);
    }

    public static Specification<WorkspaceEntity> byBranchId(UUID branchId) {
        return (root, query, cb) -> cb.equal(root.get(WorkspaceEntity.Fields.branchId), branchId);
    }

    public static Specification<WorkspaceEntity> current() {
        return (root, query, cb) -> cb.isNull(root.get(WorkspaceEntity.Fields.retiredAt));
    }

    public static Specification<WorkspaceEntity> retired() {
        return (root, query, cb) -> cb.isNotNull(root.get(WorkspaceEntity.Fields.retiredAt));
    }

    public static Specification<WorkspaceEntity> currentForBranch(UUID branchId) {
        return byBranchId(branchId).and(current());
    }
}
