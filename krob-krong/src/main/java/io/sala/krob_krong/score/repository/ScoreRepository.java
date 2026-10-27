package io.sala.krob_krong.score.repository;

import io.sala.krob_krong.score.dto.response.StudentScoreResponse;
import io.sala.krob_krong.score.entity.ScoreEntity;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScoreRepository extends JpaRepository<ScoreEntity, UUID> {
    Page<ScoreEntity> findByAssessmentId(UUID assessmentId, Pageable pageable);

    List<ScoreEntity> findByAssessmentIdAndStudentIdIn(UUID assessmentId, Collection<UUID> studentIds);

    @Query("""
    select distinct e.userId
    from EnrollmentEntity e, ClassGroupEntity g
    where g.id = e.classGroupId
    and g.schedule.courseId = :courseId
    and g.schedule.subjectId = :subjectId
    and e.userId in :studentIds
    """)
    Set<UUID> findEnrolledStudentIds(@Param("courseId") UUID courseId,
                                     @Param("subjectId") UUID subjectId,
                                     @Param("studentIds") Collection<UUID> studentIds);

    @Query(value = """
    select new io.sala.krob_krong.score.dto.response.StudentScoreResponse(
        s.studentId, s.score, s.grade, a.id, a.description)
    from ScoreEntity s, AssessmentEntity a
    where a.id = s.assessmentId
      and s.studentId = :studentId
    order by s.createdAt desc
    """,
            countQuery = """
    select count(s)
    from ScoreEntity s, AssessmentEntity a
    where a.id = s.assessmentId
      and s.studentId = :studentId
    """)
    Page<StudentScoreResponse> findByStudentId(@Param("studentId") UUID studentId, Pageable pageable);
}