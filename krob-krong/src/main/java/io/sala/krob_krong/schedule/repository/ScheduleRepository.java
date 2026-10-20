package io.sala.krob_krong.schedule.repository;

import io.sala.krob_krong.schedule.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntity, UUID> {
    boolean existsByClassroomIdAndClassGroupId(UUID classroomId, UUID classGroupId);
}
