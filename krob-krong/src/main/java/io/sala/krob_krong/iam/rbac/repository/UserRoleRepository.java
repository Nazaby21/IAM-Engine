package io.sala.krob_krong.iam.rbac.repository;

import io.sala.krob_krong.iam.rbac.entity.UserRoleEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRoleRepository
        extends JpaRepository<UserRoleEntity, UUID>, JpaSpecificationExecutor<UserRoleEntity> {}
