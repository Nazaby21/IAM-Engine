package io.sala.krob_krong.audit.repository;

import io.sala.krob_krong.audit.entity.AccessLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AccessLogRepository
        extends JpaRepository<AccessLogEntity, Long>, JpaSpecificationExecutor<AccessLogEntity> {}
