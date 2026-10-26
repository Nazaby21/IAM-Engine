package io.sala.krob_krong.enrollment.repository;

import io.sala.krob_krong.enrollment.entity.EnrollmentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PathVariable;

@Repository
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, String> {
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
