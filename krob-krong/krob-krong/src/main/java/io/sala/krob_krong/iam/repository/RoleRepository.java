package io.sala.krob_krong.iam.repository;

import io.sala.krob_krong.iam.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, String> {}
