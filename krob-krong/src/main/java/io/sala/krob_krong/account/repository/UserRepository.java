package io.sala.krob_krong.account.repository;

import io.sala.krob_krong.account.entity.UserEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = """
        select exists(
            select 1 from app_user u
            join enrollment e
                on e.user_id = u.id
            where e.user_id = :id
            and e.schedule_id = :scheduleId)
    """, nativeQuery = true)
    boolean existsByStudentId(@Param("id") UUID id, @Param("scheduleId") UUID scheduleId);
}
