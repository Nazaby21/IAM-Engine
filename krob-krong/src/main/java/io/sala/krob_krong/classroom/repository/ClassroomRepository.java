package io.sala.krob_krong.classroom.repository;

import io.sala.krob_krong.classroom.entity.ClassroomEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassroomRepository extends JpaRepository<ClassroomEntity, UUID> {
    @Query("""
            SELECT c FROM ClassroomEntity c
            WHERE c.branchId = :branchId
              AND LOWER(c.status) = 'active'
              AND LOWER(c.name) LIKE :pattern
            ORDER BY c.name ASC
            """)
    List<ClassroomEntity> findOptions(@Param("branchId") UUID branchId, @Param("pattern") String pattern);
}
