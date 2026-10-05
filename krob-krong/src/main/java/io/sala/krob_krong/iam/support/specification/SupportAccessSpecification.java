package io.sala.krob_krong.iam.support.specification;

import io.sala.krob_krong.iam.support.entity.SupportAccessEntity;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SupportAccessSpecification {

    public static Specification<SupportAccessEntity> byUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get(SupportAccessEntity.Fields.userId), userId);
    }

    public static Specification<SupportAccessEntity> bySchoolId(UUID schoolId) {
        return (root, query, cb) -> cb.equal(root.get(SupportAccessEntity.Fields.schoolId), schoolId);
    }

    public static Specification<SupportAccessEntity> notEnded() {
        return (root, query, cb) -> cb.isNull(root.get(SupportAccessEntity.Fields.endedAt));
    }

    public static Specification<SupportAccessEntity> notExpired() {
        return (root, query, cb) ->
                cb.greaterThan(root.get(SupportAccessEntity.Fields.expiresAt), Instant.now());
    }

    public static Specification<SupportAccessEntity> active() {
        return notEnded().and(notExpired());
    }

    public static Specification<SupportAccessEntity> activeForUserOnSchool(UUID userId, UUID schoolId) {
        return byUserId(userId).and(bySchoolId(schoolId)).and(active());
    }
}
