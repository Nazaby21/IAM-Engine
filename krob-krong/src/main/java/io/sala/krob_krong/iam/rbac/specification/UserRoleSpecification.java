package io.sala.krob_krong.iam.rbac.specification;

import io.sala.krob_krong.iam.rbac.entity.UserRoleEntity;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserRoleSpecification {

    public static Specification<UserRoleEntity> byUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get(UserRoleEntity.Fields.userId), userId);
    }

    public static Specification<UserRoleEntity> byRoleId(UUID roleId) {
        return (root, query, cb) -> cb.equal(root.get(UserRoleEntity.Fields.roleId), roleId);
    }

    public static Specification<UserRoleEntity> bySchoolId(UUID schoolId) {
        return (root, query, cb) -> cb.equal(root.get(UserRoleEntity.Fields.schoolId), schoolId);
    }

    public static Specification<UserRoleEntity> byBranchId(UUID branchId) {
        return (root, query, cb) -> cb.equal(root.get(UserRoleEntity.Fields.branchId), branchId);
    }

    public static Specification<UserRoleEntity> schoolWide() {
        return (root, query, cb) -> cb.isNull(root.get(UserRoleEntity.Fields.branchId));
    }

    public static Specification<UserRoleEntity> accepted() {
        return (root, query, cb) -> cb.isNotNull(root.get(UserRoleEntity.Fields.acceptedAt));
    }

    public static Specification<UserRoleEntity> pendingInvitation() {
        return (root, query, cb) -> cb.isNull(root.get(UserRoleEntity.Fields.acceptedAt));
    }

    public static Specification<UserRoleEntity> notRevoked() {
        return (root, query, cb) -> cb.isNull(root.get(UserRoleEntity.Fields.revokedAt));
    }

    public static Specification<UserRoleEntity> validAt(Instant at) {
        return (root, query, cb) -> cb.and(
                cb.lessThanOrEqualTo(root.get(UserRoleEntity.Fields.validFrom), at),
                cb.or(
                        cb.isNull(root.get(UserRoleEntity.Fields.validTo)),
                        cb.greaterThan(root.get(UserRoleEntity.Fields.validTo), at)));
    }

    public static Specification<UserRoleEntity> activeAt(Instant at) {
        return accepted().and(notRevoked()).and(validAt(at));
    }

    public static Specification<UserRoleEntity> activeNow() {
        return activeAt(Instant.now());
    }

    public static Specification<UserRoleEntity> bySource(String source) {
        return (root, query, cb) -> cb.equal(root.get(UserRoleEntity.Fields.source), source);
    }

    public static Specification<UserRoleEntity> platformGrants() {
        return (root, query, cb) -> cb.isNull(root.get(UserRoleEntity.Fields.schoolId));
    }

    public static Specification<UserRoleEntity> activeForUserInSchool(UUID userId, UUID schoolId) {
        return byUserId(userId).and(bySchoolId(schoolId)).and(activeNow());
    }
}
