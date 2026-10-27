package io.sala.krob_krong.attendance.entity.repository;

import io.sala.krob_krong.attendance.entity.ScheduleEntity;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntity, UUID> {

    List<ScheduleEntity> findByCourseIdAndSubjectId(UUID courseId, UUID subjectId);

    List<ScheduleEntity> findByCourseId(UUID courseId);

    void deleteByCourseIdAndSubjectId(UUID courseId, UUID subjectId);

    // another course_subject has a group in one of these rooms at an overlapping time
    @Query("""
    select count(s) > 0
    from ScheduleEntity s, ClassGroupEntity g
    where g.courseId = s.courseId
      and g.subjectId = s.subjectId
      and g.status = 'active'
      and g.classroomId in :roomIds
      and not (
          s.courseId = :courseId
          and s.subjectId = :subjectId
      )
      and s.dayOfWeek = :day
      and s.startedTime < :end
      and s.endedTime > :start
    """)
    boolean roomBusy(
            Collection<UUID> roomIds, UUID courseId, UUID subjectId, DayOfWeek day, LocalTime start, LocalTime end);

    // same, for instructors
    @Query("""
    select count(s) > 0
    from ScheduleEntity s, ClassGroupEntity g
    where g.courseId = s.courseId
      and g.subjectId = s.subjectId
      and g.status = 'active'
      and g.instructorId in :instructorIds
      and not (
           s.courseId = :courseId
       and s.subjectId = :subjectId
      )
      and s.dayOfWeek = :day
      and s.startedTime < :end
      and s.endedTime > :start
    """)
    boolean instructorBusy(
            Collection<UUID> instructorIds,
            UUID courseId,
            UUID subjectId,
            DayOfWeek day,
            LocalTime start,
            LocalTime end);
}
