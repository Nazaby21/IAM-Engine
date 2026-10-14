package io.sala.krob_krong.attendance.repository;

import io.sala.krob_krong.attendance.entity.AttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, UUID> {
    Optional<AttendanceEntity> findAllByStudentId(UUID studentId);
}
