package io.sala.krob_krong.assessment.repository;

import io.sala.krob_krong.assessment.entity.AssessmentEntity;
import io.sala.krob_krong.common.constant.AssessmentType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessmentRepository extends JpaRepository<AssessmentEntity, UUID> {

    Page<AssessmentEntity> findByCourseIdAndSubjectId(UUID courseId, UUID subjectId, Pageable pageable);

    Page<AssessmentEntity> findByCourseIdAndSubjectIdAndType(
            UUID courseId, UUID subjectId, AssessmentType type, Pageable pageable);

    @Query("""
        select count(a) > 0
        from AssessmentEntity a, CourseEntity c, BranchEntity b
        where a.id = :assessmentId
          and c.id = a.courseId
          and b.id = c.branchId
          and b.schoolId = :schoolId
        """)
    boolean existsInSchool(@Param("assessmentId") UUID assessmentId, @Param("schoolId") UUID schoolId);

    @Query("""
        select a
        from AssessmentEntity a, CourseEntity c, BranchEntity b
        where a.id = :assessmentId
          and c.id = a.courseId
          and b.id = c.branchId
          and b.schoolId = :schoolId
        """)
    Optional<AssessmentEntity> findInSchool(@Param("assessmentId") UUID assessmentId, @Param("schoolId") UUID schoolId);

    @Query("select count(s) > 0 from ScoreEntity s where s.assessmentId = :assessmentId")
    boolean hasScores(@Param("assessmentId") UUID assessmentId);
}
