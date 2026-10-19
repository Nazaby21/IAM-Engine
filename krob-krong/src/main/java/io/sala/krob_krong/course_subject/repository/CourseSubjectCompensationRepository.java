package io.sala.krob_krong.course_subject.repository;

import io.sala.krob_krong.course_subject.entity.CourseSubjectCompensationId;
import io.sala.krob_krong.course_subject.entity.CourseSubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseSubjectCompensationRepository
        extends JpaRepository<CourseSubjectEntity, CourseSubjectCompensationId> {}
