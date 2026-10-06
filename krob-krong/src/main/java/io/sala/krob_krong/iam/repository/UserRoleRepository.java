package io.sala.krob_krong.iam.repository;

import io.sala.krob_krong.iam.entity.UserRoleEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRoleRepository
        extends JpaRepository<UserRoleEntity, UUID>, JpaSpecificationExecutor<UserRoleEntity> {}
