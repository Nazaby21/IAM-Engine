package io.sala.krob_krong.schedule.repository;

import io.sala.krob_krong.schedule.entity.ScheduleEntity;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = "classGroups")
    List<ScheduleEntity> findByCourseIdAndSubjectId(UUID courseId, UUID subjectId);

    @EntityGraph(attributePaths = "classGroups")
    List<ScheduleEntity> findByCourseId(UUID courseId);

    @Query(value = """
            select count(*) > 0
            from schedule s
            join class_group g on g.schedule_id = s.id
            join course_subject cs on cs.course_id = s.course_id and cs.subject_id = s.subject_id
            where g.classroom_id = :roomId
              and g.status = 'active' and s.status = 'active' and cs.status = 'active'
              and not (s.course_id = :courseId and s.subject_id = :subjectId)
              and s.started_time < :end and s.ended_time > :start
              and jsonb_exists_any(s.days_of_week, string_to_array(:days, ','))
              and coalesce(cs.started_at, timestamptz '1900-01-01') <= :to
              and coalesce(cs.ended_at,   timestamptz '9999-12-31') >= :from
            """, nativeQuery = true)
    boolean roomBusy(
            @Param("roomId") UUID roomId,
            @Param("courseId") UUID courseId,
            @Param("subjectId") UUID subjectId,
            @Param("days") String days,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(value = """
    SELECT EXISTS (
        SELECT 1
        FROM schedule s
        JOIN class_group g
            ON g.schedule_id = s.id
        JOIN course_subject cs
            ON cs.course_id = s.course_id
           AND cs.subject_id = s.subject_id
        WHERE cs.status = 'active'
          AND s.status = 'active'
          AND g.status = 'active'
          AND g.instructor_id = :instructorId
          AND NOT (
              s.course_id = :courseId
              AND s.subject_id = :subjectId
          )
          AND jsonb_exists_any(
              s.days_of_week,
              CAST(:days AS text[])
          )
          AND s.started_time < :end
          AND s.ended_time > :start
          AND (cs.ended_at IS NULL OR cs.ended_at >= :from)
          AND (cs.started_at IS NULL OR cs.started_at <= :to)
    )
    """, nativeQuery = true)
    boolean instructorBusy(
            @Param("instructorId") UUID instructorId,
            @Param("courseId") UUID courseId,
            @Param("subjectId") UUID subjectId,
            @Param("days") String[] days,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
