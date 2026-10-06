package io.sala.krob_krong.school.repository;

import io.sala.krob_krong.school.entity.SchoolEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolRepository extends JpaRepository<SchoolEntity, UUID>, JpaSpecificationExecutor<SchoolEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SchoolEntity s WHERE s.id = :id")
    Optional<SchoolEntity> findByIdForUpdate(@Param("id") UUID id);
}
