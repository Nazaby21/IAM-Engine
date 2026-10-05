package io.sala.krob_krong.iam.repository;

import io.sala.krob_krong.iam.entity.RefreshTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query(
            value = "UPDATE refresh_token SET revoked_at = now() WHERE family_id = :fid AND revoked_at IS NULL",
            nativeQuery = true)
    public int revokeFamily(@Param("fid") String familyId);
}
