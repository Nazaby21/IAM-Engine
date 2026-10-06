package io.sala.krob_krong.iam.repository;

import io.sala.krob_krong.iam.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PermissionRepository
        extends JpaRepository<PermissionEntity, String>, JpaSpecificationExecutor<PermissionEntity> {}
