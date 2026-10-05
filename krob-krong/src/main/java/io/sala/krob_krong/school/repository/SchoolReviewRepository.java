package io.sala.krob_krong.school.repository;

import io.sala.krob_krong.school.entity.SchoolReviewEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SchoolReviewRepository
        extends JpaRepository<SchoolReviewEntity, UUID>, JpaSpecificationExecutor<SchoolReviewEntity> {}
