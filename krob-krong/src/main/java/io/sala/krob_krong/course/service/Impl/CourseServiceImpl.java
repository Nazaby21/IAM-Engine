package io.sala.krob_krong.course.service.Impl;

import io.sala.krob_krong.branch.repository.BranchRepository;
import io.sala.krob_krong.common.enumz.Status;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.course.dto.CourseCreateRequest;
import io.sala.krob_krong.course.dto.CourseResponse;
import io.sala.krob_krong.course.dto.CourseUpdateRequest;
import io.sala.krob_krong.course.entity.CourseEntity;
import io.sala.krob_krong.course.mapper.CourseMapper;
import io.sala.krob_krong.course.repository.CourseRepository;
import io.sala.krob_krong.course.service.CourseService;
import io.sala.krob_krong.course.specification.CourseSpecification;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final BranchRepository branchRepository;
    private static final String STATUS_ACTIVE = "ACTIVE";

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> listAllCourseInBranch(UUID branchId, PageRequest pageRequest) {
        Specification<CourseEntity> spec = CourseSpecification.byBranchId(branchId)
                .and(CourseSpecification.byStatus(STATUS_ACTIVE))
                .and(CourseSpecification.notDeleted());

        return PageResponse.from(courseRepository
                .findAll(spec, pageRequest.toSpring(Sort.by(Sort.Direction.DESC, CourseEntity.Fields.name)))
                .map(courseMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getById(UUID branchId, UUID courseId) {
        return courseMapper.toResponse(findActive(branchId, courseId));
    }

    @Override
    @Transactional
    public CourseResponse create(UUID branchId, CourseCreateRequest request) {
        if (!branchRepository.existsById(branchId)) {
            throw new BusinessException(IAMErrorCode.BRANCH_NOT_FOUND);
        }
        if (courseRepository.existsByBranchIdAndNameIgnoreCaseAndDeletedAtIsNull(branchId, request.getName())) {
            throw new BusinessException(IAMErrorCode.COURSE_ALREADY_EXISTS);
        }

        CourseEntity course = courseMapper.toEntity(request);
        course.setType(String.valueOf(request.getType()));
        course.setBranchId(branchId);
        course.setStatus(Status.ACTIVE);
        course.setCreatedBy(UUID.fromString("dd58c768-a6dd-4f59-9ab9-63ff55898834"));

        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Override
    @Transactional
    public CourseResponse update(UUID branchId, UUID courseId, CourseUpdateRequest request) {
        CourseEntity course = findActive(branchId, courseId);

        if (request.getName() != null
                && courseRepository.existsByBranchIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(
                        branchId, request.getName(), courseId)) {
            throw new BusinessException(IAMErrorCode.COURSE_ALREADY_EXISTS);
        }

        courseMapper.updateEntity(request, course);
        course.setUpdatedBy(UUID.fromString("dd58c768-a6dd-4f59-9ab9-63ff55898834"));
        course.setUpdatedAt(OffsetDateTime.now());

        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Override
    public void delete(UUID branchId, UUID courseId) {
        CourseEntity course = findActive(branchId, courseId);

        course.setDeletedAt(OffsetDateTime.now());
        course.setDeletedBy(UUID.fromString("dd58c768-a6dd-4f59-9ab9-63ff55898834"));
        courseRepository.save(course);
    }

    private CourseEntity findActive(UUID branchId, UUID courseId) {
        return courseRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(courseId, branchId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.COURSE_NOT_FOUND));
    }
}
