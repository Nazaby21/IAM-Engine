package io.sala.krob_krong.schedule.repository;

import io.sala.krob_krong.schedule.entity.ScheduleEntity;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntity, UUID> {
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM schedule s
            WHERE s.day_of_week = :dayOfWeek
              AND (
                  (
                      s.class_group_id = :classGroupId
                  )
                  OR s.classroom_id = :classroomId
              )
              AND s.started_time < :endedTime
              AND s.ended_time > :startedTime
    )
    """, nativeQuery = true)
    boolean existsScheduleConflict(
            @Param("classGroupId") UUID classGroupId,
            @Param("classroomId") UUID classroomId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("startedTime") LocalTime startedTime,
            @Param("endedTime") LocalTime endedTime);
}
