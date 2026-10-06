package io.sala.krob_krong.audit.specification;

import io.sala.krob_krong.audit.entity.AuditEventEntity;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuditEventSpecification {

    public static Specification<AuditEventEntity> bySchoolId(UUID schoolId) {
        return (root, query, cb) -> cb.equal(root.get(AuditEventEntity.Fields.schoolId), schoolId);
    }

    public static Specification<AuditEventEntity> platformEvents() {
        return (root, query, cb) -> cb.isNull(root.get(AuditEventEntity.Fields.schoolId));
    }

    public static Specification<AuditEventEntity> byActorId(UUID actorId) {
        return (root, query, cb) -> cb.equal(root.get(AuditEventEntity.Fields.actorId), actorId);
    }

    public static Specification<AuditEventEntity> byEntity(String entity) {
        return (root, query, cb) -> cb.equal(root.get(AuditEventEntity.Fields.entity), entity);
    }

    public static Specification<AuditEventEntity> byEntityId(UUID entityId) {
        return (root, query, cb) -> cb.equal(root.get(AuditEventEntity.Fields.entityId), entityId);
    }

    public static Specification<AuditEventEntity> byAction(String action) {
        return (root, query, cb) -> cb.equal(root.get(AuditEventEntity.Fields.action), action);
    }

    public static Specification<AuditEventEntity> after(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(AuditEventEntity.Fields.at), from);
    }

    public static Specification<AuditEventEntity> before(Instant to) {
        return (root, query, cb) -> cb.lessThan(root.get(AuditEventEntity.Fields.at), to);
    }

    public static Specification<AuditEventEntity> between(Instant from, Instant to) {
        return after(from).and(before(to));
    }
}
