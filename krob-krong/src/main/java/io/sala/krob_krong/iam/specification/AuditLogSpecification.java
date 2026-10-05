package io.sala.krob_krong.iam.specification;

import io.sala.krob_krong.iam.entity.AuditLogEntity;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuditLogSpecification {

    public static Specification<AuditLogEntity> byTenantId(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get(AuditLogEntity.Fields.tenantId), tenantId);
    }

    public static Specification<AuditLogEntity> byActorUserId(String actorUserId) {
        return (root, query, cb) -> cb.equal(root.get(AuditLogEntity.Fields.actorUserId), actorUserId);
    }

    public static Specification<AuditLogEntity> byAction(String action) {
        return (root, query, cb) -> cb.equal(root.get(AuditLogEntity.Fields.action), action);
    }

    public static Specification<AuditLogEntity> byTargetType(String targetType) {
        return (root, query, cb) -> cb.equal(root.get(AuditLogEntity.Fields.targetType), targetType);
    }

    public static Specification<AuditLogEntity> byTargetId(String targetId) {
        return (root, query, cb) -> cb.equal(root.get(AuditLogEntity.Fields.targetId), targetId);
    }

    public static Specification<AuditLogEntity> byTarget(String targetType, String targetId) {
        return byTargetType(targetType).and(byTargetId(targetId));
    }

    public static Specification<AuditLogEntity> createdAfter(Instant after) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(AuditLogEntity.Fields.createdAt), after);
    }

    public static Specification<AuditLogEntity> createdBefore(Instant before) {
        return (root, query, cb) -> cb.lessThan(root.get(AuditLogEntity.Fields.createdAt), before);
    }

    public static Specification<AuditLogEntity> createdBetween(Instant from, Instant to) {
        return createdAfter(from).and(createdBefore(to));
    }
}
