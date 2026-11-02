package io.sala.krob_krong.classroom.repository;

import io.sala.krob_krong.classroom.entity.ClassGroupEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassGroupRepository extends JpaRepository<ClassGroupEntity, UUID> {}
