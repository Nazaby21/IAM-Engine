package io.sala.krob_krong.classroom.repository;

import io.sala.krob_krong.classroom.entity.ClassroomEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassroomRepository extends JpaRepository<ClassroomEntity, UUID> {}
