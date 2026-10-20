package io.sala.krob_krong.classroom.repository;

import io.sala.krob_krong.classroom.entity.ClassGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClassGroupRepository extends JpaRepository<ClassGroupEntity, UUID> {}
