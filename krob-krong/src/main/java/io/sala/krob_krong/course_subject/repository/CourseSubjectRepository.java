package io.sala.krob_krong.course_subject.repository;

import io.sala.krob_krong.course_subject.entity.CourseSubjectCompensationId;
import io.sala.krob_krong.course_subject.entity.CourseSubjectEntity;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseSubjectRepository
        extends JpaRepository<CourseSubjectEntity, CourseSubjectCompensationId>,
                JpaSpecificationExecutor<CourseSubjectEntity> {
    boolean existsByIdCourseIdAndCode(
            UUID courseId, @Size(max = 50, message = "Code must not exceed 50 characters") String code);

    boolean existsByIdCourseIdAndCodeAndIdSubjectIdNot(UUID courseId, String code, UUID subjectId);

    @Query(value = """
        SELECT EXISTS (SELECT 1 FROM class_group WHERE course_id = :courseId AND subject_id = :subjectId)
            OR EXISTS (SELECT 1 FROM assessment  WHERE course_id = :courseId AND subject_id = :subjectId)
        """, nativeQuery = true)
    boolean isInUse(@Param("courseId") UUID courseId, @Param("subjectId") UUID subjectId);

    @Override
    @EntityGraph(attributePaths = {"subject", "instructor"})
    Page<CourseSubjectEntity> findAll(Specification<CourseSubjectEntity> spec, Pageable pageable);
}
