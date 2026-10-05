package io.sala.krob_krong.branch.repository;

import io.sala.krob_krong.branch.entity.BranchEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BranchRepository extends JpaRepository<BranchEntity, UUID>, JpaSpecificationExecutor<BranchEntity> {}
