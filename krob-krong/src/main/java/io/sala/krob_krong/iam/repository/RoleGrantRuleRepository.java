package io.sala.krob_krong.iam.repository;

import io.sala.krob_krong.iam.entity.RoleGrantRuleEntity;
import io.sala.krob_krong.iam.entity.RoleGrantRuleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoleGrantRuleRepository
        extends JpaRepository<RoleGrantRuleEntity, RoleGrantRuleId>, JpaSpecificationExecutor<RoleGrantRuleEntity> {}
