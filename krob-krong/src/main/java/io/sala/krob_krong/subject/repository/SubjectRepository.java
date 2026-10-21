package io.sala.krob_krong.subject.repository;

import io.sala.krob_krong.subject.entity.SubjectEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<SubjectEntity, UUID>, JpaSpecificationExecutor<SubjectEntity> {
    boolean existsByBranchIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID branchId, String name);

    Optional<SubjectEntity> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    @Query("""
        SELECT COUNT(s) > 0 FROM SubjectEntity s
        WHERE s.branchId = :branchId
          AND LOWER(s.name) = LOWER(:name)
          AND s.id <> :excludeId
          AND s.deletedAt IS NULL
        """)
    boolean existsDuplicateName(
            @Param("branchId") UUID branchId, @Param("name") String name, @Param("excludeId") UUID excludeId);

    Optional<SubjectEntity> findByIdAndDeletedAtIsNull(UUID id);
}
