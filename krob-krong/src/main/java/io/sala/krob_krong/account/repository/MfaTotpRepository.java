package io.sala.krob_krong.account.repository;

import io.sala.krob_krong.account.entity.MfaTotpEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MfaTotpRepository extends JpaRepository<MfaTotpEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM MfaTotpEntity t WHERE t.userId = :userId")
    Optional<MfaTotpEntity> findByIdForUpdate(@Param("userId") UUID userId);
}
