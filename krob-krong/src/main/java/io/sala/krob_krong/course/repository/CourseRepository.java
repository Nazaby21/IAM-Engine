package io.sala.krob_krong.course.repository;

import io.sala.krob_krong.course.entity.CourseEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, UUID>, JpaSpecificationExecutor<CourseEntity> {
    Optional<CourseEntity> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    boolean existsByBranchIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID branchId, String name);

    boolean existsByBranchIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(UUID branchId, String name, UUID id);

    Optional<CourseEntity> findByIdAndDeletedAtIsNull(UUID id);
}
