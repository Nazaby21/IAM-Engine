package io.sala.krob_krong.iam.account.specification;

import io.sala.krob_krong.iam.account.entity.RefreshTokenEntity;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RefreshTokenSpecification {

    public static Specification<RefreshTokenEntity> byTokenHash(String tokenHash) {
        return (root, query, cb) -> cb.equal(root.get(RefreshTokenEntity.Fields.tokenHash), tokenHash);
    }

    public static Specification<RefreshTokenEntity> byUserId(String userId) {
        return (root, query, cb) -> cb.equal(root.get(RefreshTokenEntity.Fields.userId), userId);
    }

    public static Specification<RefreshTokenEntity> byTenantId(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get(RefreshTokenEntity.Fields.tenantId), tenantId);
    }

    public static Specification<RefreshTokenEntity> byFamilyId(String familyId) {
        return (root, query, cb) -> cb.equal(root.get(RefreshTokenEntity.Fields.familyId), familyId);
    }

    public static Specification<RefreshTokenEntity> notRevoked() {
        return (root, query, cb) -> cb.isNull(root.get(RefreshTokenEntity.Fields.revokedAt));
    }

    public static Specification<RefreshTokenEntity> notExpired() {
        return (root, query, cb) ->
                cb.greaterThan(root.get(RefreshTokenEntity.Fields.expiresAt), Instant.now());
    }

    public static Specification<RefreshTokenEntity> active() {
        return notRevoked().and(notExpired());
    }

    public static Specification<RefreshTokenEntity> expiredBefore(Instant cutoff) {
        return (root, query, cb) -> cb.lessThan(root.get(RefreshTokenEntity.Fields.expiresAt), cutoff);
    }
}
