package io.sala.krob_krong.account.repository;

import io.sala.krob_krong.account.entity.RefreshTokenEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshTokenEntity, UUID>, JpaSpecificationExecutor<RefreshTokenEntity> {

    // REQUIRES_NEW: reuse detection revokes and then throws, which must not roll the revocation back.
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :now WHERE r.familyId = :familyId AND r.revokedAt IS NULL")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

    // Conditional on revokedAt IS NULL so that, of two concurrent refreshes with the same token, only one wins.
    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :now, r.replacedBy = :replacedBy "
            + "WHERE r.id = :id AND r.revokedAt IS NULL")
    int rotate(@Param("id") UUID id, @Param("replacedBy") UUID replacedBy, @Param("now") Instant now);
}
