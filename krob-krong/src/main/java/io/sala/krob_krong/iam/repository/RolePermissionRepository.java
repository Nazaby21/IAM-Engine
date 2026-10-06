package io.sala.krob_krong.iam.repository;

import io.sala.krob_krong.iam.entity.RolePermissionEntity;
import io.sala.krob_krong.iam.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RolePermissionRepository
        extends JpaRepository<RolePermissionEntity, RolePermissionId>, JpaSpecificationExecutor<RolePermissionEntity> {}
