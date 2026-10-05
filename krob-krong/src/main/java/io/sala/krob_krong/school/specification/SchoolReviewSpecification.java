package io.sala.krob_krong.school.specification;

import io.sala.krob_krong.school.entity.SchoolReviewEntity;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SchoolReviewSpecification {

    public static Specification<SchoolReviewEntity> bySchoolId(UUID schoolId) {
        return (root, query, cb) -> cb.equal(root.get(SchoolReviewEntity.Fields.schoolId), schoolId);
    }

    public static Specification<SchoolReviewEntity> pending() {
        return (root, query, cb) -> cb.isNull(root.get(SchoolReviewEntity.Fields.decidedAt));
    }

    public static Specification<SchoolReviewEntity> decided() {
        return (root, query, cb) -> cb.isNotNull(root.get(SchoolReviewEntity.Fields.decidedAt));
    }

    public static Specification<SchoolReviewEntity> byDecision(String decision) {
        return (root, query, cb) -> cb.equal(root.get(SchoolReviewEntity.Fields.decision), decision);
    }

    public static Specification<SchoolReviewEntity> pendingForSchool(UUID schoolId) {
        return bySchoolId(schoolId).and(pending());
    }
}
