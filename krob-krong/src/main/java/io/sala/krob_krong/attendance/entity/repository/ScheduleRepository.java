package io.sala.krob_krong.attendance.entity.repository;

import io.sala.krob_krong.attendance.entity.ScheduleEntity;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntity, UUID> {

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

    @Query("""
    select count(s) > 0
    from ScheduleEntity s
      join s.classGroups g
      join s.daysOfWeek d
      join CourseSubjectEntity cs
        on cs.id.courseId = s.courseId and cs.id.subjectId = s.subjectId
    where cs.status = 'active'
      and s.status = 'active'
      and g.status = 'active'
      and g.instructorId = :instructorId
      and not (s.courseId = :courseId and s.subjectId = :subjectId)
      and d in :days
      and s.startedTime < :end
      and s.endedTime > :start
      and (cs.endedAt is null or cs.endedAt >= :from)
      and (cs.startedAt is null or cs.startedAt <= :to)
    """)
    boolean instructorBusy(
            UUID instructorId,
            UUID courseId,
            UUID subjectId,
            Collection<DayOfWeek> days,
            LocalTime start,
            LocalTime end,
            Instant from,
            Instant to);
}
