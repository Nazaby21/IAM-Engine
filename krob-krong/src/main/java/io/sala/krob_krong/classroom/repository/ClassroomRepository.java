package io.sala.krob_krong.classroom.repository;

import io.sala.krob_krong.classroom.entity.ClassroomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassroomRepository extends JpaRepository<ClassroomEntity, UUID> {
    Optional<ClassroomEntity>  findByIdAndBranchId(UUID classroomId, UUID branchId);
    List<ClassroomEntity> findAllByBranchIdOrderByIdAsc(UUID branchId);
}
