package io.sala.krob_krong.iam.rbac.repository;

import io.sala.krob_krong.iam.rbac.entity.RoleGrantRuleEntity;
import io.sala.krob_krong.iam.rbac.entity.RoleGrantRuleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoleGrantRuleRepository
        extends JpaRepository<RoleGrantRuleEntity, RoleGrantRuleId>,
                JpaSpecificationExecutor<RoleGrantRuleEntity> {}
