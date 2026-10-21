package io.sala.krob_krong.course_subject.repository;

import io.sala.krob_krong.account.entity.UserEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
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

    @Query("""
        SELECT u FROM UserEntity u
        WHERE u.schoolId = (SELECT b.schoolId FROM BranchEntity b WHERE b.id = :branchId)
          AND u.roleId = (SELECT r.id FROM RoleEntity r WHERE r.code = 'instructor')
          AND (LOWER(u.firstName) LIKE :pattern
               OR LOWER(u.lastName) LIKE :pattern
               OR LOWER(u.email)    LIKE :pattern)
        ORDER BY u.firstName ASC, u.lastName ASC
        """)
    List<UserEntity> findInstructors(@Param("branchId") UUID branchId, @Param("pattern") String pattern);
}
