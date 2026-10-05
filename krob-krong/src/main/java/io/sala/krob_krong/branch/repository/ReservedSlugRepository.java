package io.sala.krob_krong.branch.repository;

import io.sala.krob_krong.branch.entity.ReservedSlugEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservedSlugRepository extends JpaRepository<ReservedSlugEntity, String> {}
