package io.sala.krob_krong.iam.repository;

import io.sala.krob_krong.iam.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshTokenEntity, String>, JpaSpecificationExecutor<RefreshTokenEntity> {

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = CURRENT_TIMESTAMP "
            + "WHERE r.familyId = :fid AND r.revokedAt IS NULL")
    void revokeFamily(@Param("fid") String familyId);
}
