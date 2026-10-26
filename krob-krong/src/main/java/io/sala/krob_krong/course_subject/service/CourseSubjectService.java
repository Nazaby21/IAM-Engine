package io.sala.krob_krong.course_subject.service;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.course_subject.dto.request.CourseSubjectCreateRequest;
import io.sala.krob_krong.course_subject.dto.request.CourseSubjectUpdateRequest;
import io.sala.krob_krong.course_subject.dto.response.CourseSubjectResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public interface CourseSubjectService {
    PageResponse<CourseSubjectResponse> listAllSubjectInCourse(UUID courseId, PageRequest page);

    CourseSubjectResponse create(UUID courseId, CourseSubjectCreateRequest request);

    CourseSubjectResponse update(UUID courseId, UUID subjectId, CourseSubjectUpdateRequest request);

    void remove(UUID courseId, UUID subjectId);
}
