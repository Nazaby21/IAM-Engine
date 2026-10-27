package io.sala.krob_krong.enrollment.repository;

import io.lettuce.core.dynamic.annotation.Param;
import io.sala.krob_krong.enrollment.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, String> {
    @Query("""
    SELECT COUNT(e)
    FROM EnrollmentEntity e
    WHERE e.classGroupId = :classGroupId
      AND e.status IN :statuses
    """)
    long countOccupiedSeats(
            @Param("classGroupId") UUID classGroupId,
            @Param("statuses") Collection<String> statuses
    );
}
