package io.sala.krob_krong.classroom.repository;

import io.sala.krob_krong.classroom.entity.ClassGroupEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassGroupRepository extends JpaRepository<ClassGroupEntity, UUID> {
    List<ClassGroupEntity> findByScheduleId(UUID scheduleId);
}
