package io.sala.krob_krong.audit.specification;

import io.sala.krob_krong.audit.entity.AccessLogEntity;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AccessLogSpecification {

    public static Specification<AccessLogEntity> byUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get(AccessLogEntity.Fields.userId), userId);
    }

    public static Specification<AccessLogEntity> byPermissionCode(String code) {
        return (root, query, cb) -> cb.equal(root.get(AccessLogEntity.Fields.permissionCode), code);
    }

    public static Specification<AccessLogEntity> bySchoolId(UUID schoolId) {
        return (root, query, cb) -> cb.equal(root.get(AccessLogEntity.Fields.schoolId), schoolId);
    }

    public static Specification<AccessLogEntity> allowed() {
        return (root, query, cb) -> cb.isTrue(root.get(AccessLogEntity.Fields.allowed));
    }

    public static Specification<AccessLogEntity> denied() {
        return (root, query, cb) -> cb.isFalse(root.get(AccessLogEntity.Fields.allowed));
    }

    public static Specification<AccessLogEntity> after(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(AccessLogEntity.Fields.at), from);
    }

    public static Specification<AccessLogEntity> before(Instant to) {
        return (root, query, cb) -> cb.lessThan(root.get(AccessLogEntity.Fields.at), to);
    }
}
