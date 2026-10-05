package io.sala.krob_krong.branch.repository;

import io.sala.krob_krong.branch.entity.WorkspaceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WorkspaceRepository
        extends JpaRepository<WorkspaceEntity, UUID>, JpaSpecificationExecutor<WorkspaceEntity> {}
