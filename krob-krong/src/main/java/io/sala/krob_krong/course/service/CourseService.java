package io.sala.krob_krong.course.service;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.course.dto.CourseCreateRequest;
import io.sala.krob_krong.course.dto.CourseResponse;
import io.sala.krob_krong.course.dto.CourseUpdateRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public interface CourseService {
    PageResponse<CourseResponse> listAllCourseInBranch(UUID branchId, PageRequest pageRequest);

    CourseResponse getById(UUID branchId, UUID courseId);

    CourseResponse create(UUID branchId, CourseCreateRequest request);

    CourseResponse update(UUID branchId, UUID courseId, CourseUpdateRequest request);

    void delete(UUID branchId, UUID courseId);
}
