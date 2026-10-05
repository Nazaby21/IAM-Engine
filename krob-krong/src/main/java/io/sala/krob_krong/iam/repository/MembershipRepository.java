package io.sala.krob_krong.iam.repository;

import io.sala.krob_krong.iam.entity.MembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MembershipRepository
        extends JpaRepository<MembershipEntity, String>, JpaSpecificationExecutor<MembershipEntity> {}
