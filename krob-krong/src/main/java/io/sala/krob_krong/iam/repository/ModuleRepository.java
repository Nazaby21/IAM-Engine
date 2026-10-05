package io.sala.krob_krong.iam.repository;

import io.sala.krob_krong.iam.entity.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ModuleRepository
        extends JpaRepository<ModuleEntity, String>, JpaSpecificationExecutor<ModuleEntity> {}
