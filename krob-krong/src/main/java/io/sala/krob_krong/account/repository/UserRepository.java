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
            SELECT EXISTS (
                SELECT 1
                FROM app_user u
                JOIN enrollment e
                    ON e.user_id = u.id
                JOIN class_group cg
                    ON cg.id = e.class_group_id
                WHERE u.id = :studentId
                  AND cg.schedule_id = :scheduleId
            )
            """, nativeQuery = true)
    boolean existsByStudentIdAndScheduleId(@Param("studentId") UUID studentId, @Param("scheduleId") UUID scheduleId);
}
