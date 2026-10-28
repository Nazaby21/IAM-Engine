package io.sala.krob_krong.attendance.repository;

import io.sala.krob_krong.attendance.entity.AttendanceEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, UUID>, JpaSpecificationExecutor<AttendanceEntity> {
    List<AttendanceEntity> findByStudentId(UUID studentId);

    @Query(value = """
        select exists(
                select 1 from attendance a
                    where a.attendance_date = :attendanceDate
                    and a.schedule_id = :scheduleId
                    and a.student_id = :studentId
            )
    """, nativeQuery = true)
    boolean existsAttendance(
            @Param("attendanceDate") LocalDate attendanceDate,
            @Param("studentId") UUID studentId,
            @Param("scheduleId") UUID scheduleId);

    @Query(value = """
                                select * from attendance a
                                    where a.attendance_date = :attendanceDate
                                    and a.schedule_id = :scheduleId
                                    and a.student_id = :studentId
            """, nativeQuery = true)
    Optional<AttendanceEntity> findAttendance(
            @Param("attendanceDate") LocalDate attendanceDate,
            @Param("studentId") UUID studentId,
            @Param("scheduleId") UUID scheduleId);
}
