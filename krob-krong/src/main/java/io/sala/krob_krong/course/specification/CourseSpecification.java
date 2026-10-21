package io.sala.krob_krong.course.specification;

import io.sala.krob_krong.course.entity.CourseEntity;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourseSpecification {
    public static Specification<CourseEntity> byBranchId(UUID branchId) {
        return (root, query, cb) -> cb.equal(root.get(CourseEntity.Fields.branchId), branchId);
    }

    public static Specification<CourseEntity> byStatus(String status) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get(CourseEntity.Fields.status)), status.toLowerCase(Locale.ROOT));
    }

    public static Specification<CourseEntity> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get(CourseEntity.Fields.deletedAt));
    }
}
