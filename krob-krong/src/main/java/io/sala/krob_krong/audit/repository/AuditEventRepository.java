package io.sala.krob_krong.audit.repository;

import io.sala.krob_krong.audit.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEventRepository
        extends JpaRepository<AuditEventEntity, Long>, JpaSpecificationExecutor<AuditEventEntity> {}
