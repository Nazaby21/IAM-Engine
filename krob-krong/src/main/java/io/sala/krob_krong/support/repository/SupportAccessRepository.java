package io.sala.krob_krong.support.repository;

import io.sala.krob_krong.support.entity.SupportAccessEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupportAccessRepository
        extends JpaRepository<SupportAccessEntity, UUID>, JpaSpecificationExecutor<SupportAccessEntity> {}
