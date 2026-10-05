package io.sala.krob_krong.iam.repository;

import io.sala.krob_krong.iam.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TenantRepository
        extends JpaRepository<TenantEntity, String>, JpaSpecificationExecutor<TenantEntity> {}
