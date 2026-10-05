package io.sala.krob_krong.iam.repository;

import io.sala.krob_krong.iam.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository
        extends JpaRepository<AuditLogEntity, String>, JpaSpecificationExecutor<AuditLogEntity> {}
