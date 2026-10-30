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

    // AFTER: assessments, and students enrolled in any group of this course_subject
    @Query("""
    select count(a) from AssessmentEntity a
    where a.courseId = :courseId and a.subjectId = :subjectId
    """)
    long countAssessments(UUID courseId, UUID subjectId);

    default boolean isInUse(UUID courseId, UUID subjectId) {
        return countAssessments(courseId, subjectId) > 0;
    }

    @Override
    @EntityGraph(attributePaths = {"subject"})
    Page<CourseSubjectEntity> findAll(Specification<CourseSubjectEntity> spec, Pageable pageable);

    @Query("""
    select count(cs) > 0
    from CourseSubjectEntity cs, CourseEntity c, BranchEntity b
    where cs.id.courseId = :courseId
      and cs.id.subjectId = :subjectId
      and c.id = cs.id.courseId
      and b.id = c.branchId
      and b.schoolId = :schoolId
    """)
    boolean existsInSchool(
            @Param("courseId") UUID courseId, @Param("subjectId") UUID subjectId, @Param("schoolId") UUID schoolId);
}
