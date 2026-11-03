package io.sala.krob_krong.enrollment.repository;

import io.lettuce.core.dynamic.annotation.Param;
import io.sala.krob_krong.enrollment.entity.EnrollmentEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PathVariable;

@Repository
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, String> {
    @Query("""
            SELECT COUNT(e)
            FROM EnrollmentEntity e
            WHERE e.classGroupId = :classGroupId
              AND e.status IN :statuses
            """)
    long countOccupiedSeats(@Param("classGroupId") UUID classGroupId, @Param("statuses") Collection<String> statuses);

    @Query(value = """
                select e.* from enrollment e
                join schedule s
                on s.class_group_id = e.class_group_id
                where s.id = :scheduleId
                and e.user_id in (:studentIds)
                and e.status = 'COMPLETED'
            """, nativeQuery = true)
    List<EnrollmentEntity> findAllEnrollmentByScheduleId(
            @PathVariable("scheduleId") UUID scheduleId, @PathVariable("studentIds") List<UUID> studentIds);
}
