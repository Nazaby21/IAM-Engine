package io.sala.krob_krong.attendance.repository;

import io.sala.krob_krong.attendance.entity.AttendanceEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, UUID> {
    @Query(value = """
            select exists(
                select 1 from attendance a
                where a.schedule_id = :scheduleId
                and a.enrollment_id in (:enrollmentIds)
                and a.attendance_date = :attendanceDate
            )
            """, nativeQuery = true)
    boolean existsAttendance(
            @Param("scheduleId") UUID scheduleId,
            @Param("enrollmentIds") List<UUID> enrollmentIds,
            LocalDate attendanceDate);
}
