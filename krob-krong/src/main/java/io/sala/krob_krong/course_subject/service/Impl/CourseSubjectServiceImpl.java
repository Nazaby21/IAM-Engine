package io.sala.krob_krong.course_subject.service.Impl;

import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.attendance.entity.ScheduleEntity;
import io.sala.krob_krong.attendance.entity.repository.ScheduleRepository;
import io.sala.krob_krong.classroom.entity.ClassGroupEntity;
import io.sala.krob_krong.classroom.entity.ClassroomEntity;
import io.sala.krob_krong.classroom.repository.ClassGroupRepository;
import io.sala.krob_krong.classroom.repository.ClassroomRepository;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.course.entity.CourseEntity;
import io.sala.krob_krong.course.repository.CourseRepository;
import io.sala.krob_krong.course_subject.dto.request.ClassGroupRequest;
import io.sala.krob_krong.course_subject.dto.request.CourseSubjectCreateRequest;
import io.sala.krob_krong.course_subject.dto.request.CourseSubjectUpdateRequest;
import io.sala.krob_krong.course_subject.dto.request.ScheduleRequest;
import io.sala.krob_krong.course_subject.dto.response.ClassGroupResponse;
import io.sala.krob_krong.course_subject.dto.response.CourseSubjectResponse;
import io.sala.krob_krong.course_subject.dto.response.InstructorResponse;
import io.sala.krob_krong.course_subject.entity.CourseSubjectCompensationId;
import io.sala.krob_krong.course_subject.entity.CourseSubjectEntity;
import io.sala.krob_krong.course_subject.mapper.CourseSubjectMapper;
import io.sala.krob_krong.course_subject.repository.CourseSubjectRepository;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.course_subject.service.CourseSubjectService;
import io.sala.krob_krong.course_subject.specification.CourseSubjectListSpec;
import io.sala.krob_krong.course_subject.validator.ScheduleValidator;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.subject.entity.SubjectEntity;
import io.sala.krob_krong.subject.repository.SubjectRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseSubjectServiceImpl implements CourseSubjectService {

    private static final String STATUS_ACTIVE = "active";

    private final CourseSubjectRepository courseSubjectRepository;
    private final CourseSubjectMapper courseSubjectMapper;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final ClassGroupRepository classGroupRepository;
    private final ClassroomRepository classroomRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleValidator scheduleValidator;

    // ---------- queries ----------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseSubjectResponse> listAllSubjectInCourse(UUID courseId, PageRequest page) {
        Specification<CourseSubjectEntity> spec = CourseSubjectListSpec.byCourseId(courseId)
                .and(CourseSubjectListSpec.byStatus(STATUS_ACTIVE))
                .and(CourseSubjectListSpec.courseAndSubjectNotDeleted());

        Pageable pageable = page.toSpring(Sort.by(Sort.Direction.DESC, CourseSubjectEntity.Fields.code));

        Map<UUID, List<ClassGroupEntity>> groupsBySubject = classGroupRepository.findByCourseId(courseId).stream()
                .collect(Collectors.groupingBy(ClassGroupEntity::getSubjectId));

        Map<UUID, List<ScheduleEntity>> schedulesBySubject = scheduleRepository.findByCourseId(courseId).stream()
                .collect(Collectors.groupingBy(ScheduleEntity::getSubjectId));

        return PageResponse.from(courseSubjectRepository.findAll(spec, pageable).map(e -> {
            UUID sid = e.getId().getSubjectId();
            return toResponse(
                    e, groupsBySubject.getOrDefault(sid, List.of()), schedulesBySubject.getOrDefault(sid, List.of()));
        }));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorResponse> listInstructors(UUID branchId, String keyword) {
        String pattern = (keyword == null || keyword.isBlank())
                ? "%"
                : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";

        return userRepository.findInstructors(branchId, pattern).stream()
                .map(courseSubjectMapper::toInstructor)
                .toList();
    }

    // ---------- commands ----------

    @Override
    @Transactional
    public CourseSubjectResponse create(UUID courseId, CourseSubjectCreateRequest request) {
        UUID subjectId = request.getSubjectId();
        CourseEntity course = findCourse(courseId);

        SubjectEntity subject = subjectRepository
                .findByIdAndDeletedAtIsNull(subjectId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.SUBJECT_NOT_FOUND));

        if (!Objects.equals(course.getBranchId(), subject.getBranchId())) {
            throw new BusinessException(IAMErrorCode.SUBJECT_NOT_IN_COURSE_BRANCH);
        }

        var id = new CourseSubjectCompensationId(courseId, subjectId);
        if (courseSubjectRepository.existsById(id)) {
            throw new BusinessException(IAMErrorCode.COURSE_SUBJECT_ALREADY_EXISTS);
        }
        if (request.getCode() != null
                && courseSubjectRepository.existsByIdCourseIdAndCode(courseId, request.getCode())) {
            throw new BusinessException(IAMErrorCode.COURSE_SUBJECT_CODE_ALREADY_EXISTS);
        }

        // validate groups and schedules BEFORE saving anything
        List<ClassGroupEntity> groups = buildClassGroups(
                course, id, request.getClassGroups(), request.getInstructorId(), request.getMaxStudents(), List.of());
        List<ScheduleEntity> schedules = buildSchedules(id, request.getSchedules(), List.of(), groups);

        CourseSubjectEntity entity = courseSubjectMapper.toEntity(request);
        entity.setId(id);
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        entity.setSubject(subject);
        entity.setInstructor(findInstructor(request.getInstructorId()));

        // class_group and schedule have FKs to course_subject, so insert the link first
        CourseSubjectEntity saved = courseSubjectRepository.saveAndFlush(entity);
        return toResponse(saved, classGroupRepository.saveAll(groups), scheduleRepository.saveAll(schedules));
    }

    @Override
    @Transactional
    public CourseSubjectResponse update(UUID courseId, UUID subjectId, CourseSubjectUpdateRequest request) {
        CourseEntity course = findCourse(courseId);
        CourseSubjectEntity entity = findLink(courseId, subjectId);

        if (request.getCode() != null
                && courseSubjectRepository.existsByIdCourseIdAndCodeAndIdSubjectIdNot(
                        courseId, request.getCode(), subjectId)) {
            throw new BusinessException(IAMErrorCode.COURSE_SUBJECT_CODE_ALREADY_EXISTS);
        }

        courseSubjectMapper.updateEntity(request, entity);

        if (request.getStatus() != null) {
            entity.setStatus(normalizeStatus(request.getStatus()));
        }
        if (request.getInstructorId() != null) {
            entity.setInstructor(findInstructor(request.getInstructorId()));
        }
        if (entity.getStartedAt() != null
                && entity.getEndedAt() != null
                && entity.getEndedAt().isBefore(entity.getStartedAt())) {
            throw new BusinessException(IAMErrorCode.COURSE_SUBJECT_INVALID_DATES);
        }

        // incoming groups are validated together with the existing ones
        List<ClassGroupEntity> existing = classGroupRepository.findByCourseIdAndSubjectId(courseId, subjectId);
        List<ClassGroupEntity> incoming = buildClassGroups(
                course,
                entity.getId(),
                request.getClassGroups(),
                entity.getInstructorId(),
                entity.getMaxStudents(),
                existing);

        // schedules are validated against every group (existing + edited/new)
        List<ClassGroupEntity> effectiveGroups =
                Stream.concat(existing.stream(), incoming.stream()).distinct().toList();
        List<ScheduleEntity> existingSchedules = scheduleRepository.findByCourseIdAndSubjectId(courseId, subjectId);
        List<ScheduleEntity> incomingSchedules =
                buildSchedules(entity.getId(), request.getSchedules(), existingSchedules, effectiveGroups);

        CourseSubjectEntity saved = courseSubjectRepository.saveAndFlush(entity);
        classGroupRepository.saveAll(incoming);
        scheduleRepository.saveAll(incomingSchedules);

        // re-read so updated rows are not duplicated in the response
        return toResponse(
                saved,
                classGroupRepository.findByCourseIdAndSubjectId(courseId, subjectId),
                scheduleRepository.findByCourseIdAndSubjectId(courseId, subjectId));
    }

    @Override
    @Transactional
    public void remove(UUID courseId, UUID subjectId) {
        CourseSubjectEntity entity = findLink(courseId, subjectId);

        if (courseSubjectRepository.isInUse(courseId, subjectId)) {
            throw new BusinessException(IAMErrorCode.COURSE_SUBJECT_IN_USE);
        }
        scheduleRepository.deleteByCourseIdAndSubjectId(courseId, subjectId);
        courseSubjectRepository.delete(entity);
    }

    // ---------- class group building & validation ----------

    private List<ClassGroupEntity> buildClassGroups(
            CourseEntity course,
            CourseSubjectCompensationId id,
            List<ClassGroupRequest> inputs,
            UUID defaultInstructorId,
            Integer maxStudents,
            List<ClassGroupEntity> existing) {

        if (inputs == null || inputs.isEmpty()) {
            // no groups sent: still make sure existing groups can hold max_students
            if (maxStudents != null && !existing.isEmpty()) {
                int total = existing.stream()
                        .mapToInt(ClassGroupEntity::getCapacity)
                        .sum();
                if (total < maxStudents) {
                    throw new BusinessException(IAMErrorCode.CLASS_GROUPS_CANNOT_HOLD_STUDENTS);
                }
            }
            return List.of();
        }

        Set<UUID> updatedIds = inputs.stream()
                .map(ClassGroupRequest::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<ClassGroupEntity> untouched =
                existing.stream().filter(g -> !updatedIds.contains(g.getId())).toList();

        validateNames(inputs, untouched);
        validateTotalCapacity(inputs, untouched, maxStudents);
        validateInstructors(inputs, defaultInstructorId);
        validateClassrooms(inputs, untouched);

        Map<UUID, ClassGroupEntity> existingById =
                existing.stream().collect(Collectors.toMap(ClassGroupEntity::getId, Function.identity()));
        UUID userId = currentUserId();

        return inputs.stream()
                .map(in -> {
                    validateClassroom(in, course);

                    ClassGroupEntity g = in.getId() == null
                            ? newGroup(id, userId)
                            : Objects.requireNonNull(existingById.get(in.getId()), () -> {
                                throw new BusinessException(IAMErrorCode.CLASS_GROUP_NOT_FOUND);
                            });

                    g.setName(in.getName().trim());
                    g.setCapacity(in.getCapacity());
                    g.setClassroomId(in.getClassroomId());
                    g.setInstructorId(resolveInstructorId(in, defaultInstructorId));
                    return g;
                })
                .toList();
    }

    private void validateNames(List<ClassGroupRequest> inputs, List<ClassGroupEntity> untouched) {
        Set<String> seen = new HashSet<>();
        Stream.concat(
                        untouched.stream().map(ClassGroupEntity::getName),
                        inputs.stream().map(ClassGroupRequest::getName))
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .filter(name -> !seen.add(name))
                .findAny()
                .ifPresent(name -> {
                    throw new BusinessException(IAMErrorCode.CLASS_GROUP_NAME_DUPLICATED);
                });
    }

    private void validateTotalCapacity(
            List<ClassGroupRequest> inputs, List<ClassGroupEntity> untouched, Integer maxStudents) {
        if (maxStudents == null) {
            return;
        }
        int total = untouched.stream().mapToInt(ClassGroupEntity::getCapacity).sum()
                + inputs.stream().mapToInt(ClassGroupRequest::getCapacity).sum();

        if (total < maxStudents) {
            throw new BusinessException(IAMErrorCode.CLASS_GROUPS_CANNOT_HOLD_STUDENTS);
        }
    }

    private void validateInstructors(List<ClassGroupRequest> inputs, UUID defaultInstructorId) {
        List<UUID> ids = inputs.stream()
                .map(in -> resolveInstructorId(in, defaultInstructorId))
                .filter(Objects::nonNull)
                .toList();

        Set<UUID> unique = new HashSet<>(ids);
        if (unique.size() != ids.size()) {
            throw new BusinessException(IAMErrorCode.CLASS_GROUP_INSTRUCTOR_DUPLICATED);
        }
        if (userRepository.findAllById(unique).size() != unique.size()) {
            throw new BusinessException(IAMErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateClassroom(ClassGroupRequest in, CourseEntity course) {
        if (in.getClassroomId() == null) {
            return;
        }
        ClassroomEntity room = classroomRepository
                .findById(in.getClassroomId())
                .filter(r -> STATUS_ACTIVE.equalsIgnoreCase(r.getStatus()))
                .orElseThrow(() -> new BusinessException(IAMErrorCode.CLASSROOM_NOT_FOUND));

        if (!Objects.equals(room.getBranchId(), course.getBranchId())) {
            throw new BusinessException(IAMErrorCode.CLASSROOM_NOT_IN_COURSE_BRANCH);
        }
        if (room.getCapacity() != null && in.getCapacity() > room.getCapacity()) {
            throw new BusinessException(IAMErrorCode.CLASS_GROUP_EXCEEDS_ROOM_CAPACITY);
        }
    }

    private void validateClassrooms(List<ClassGroupRequest> inputs, List<ClassGroupEntity> untouched) {
        Set<UUID> seen = new HashSet<>();
        Stream.concat(
                        untouched.stream().map(ClassGroupEntity::getClassroomId),
                        inputs.stream().map(ClassGroupRequest::getClassroomId))
                .filter(Objects::nonNull) // groups without a room are allowed
                .filter(roomId -> !seen.add(roomId)) // add() returns false if already seen
                .findAny()
                .ifPresent(roomId -> {
                    throw new BusinessException(IAMErrorCode.CLASS_GROUP_CLASSROOM_DUPLICATED);
                });
    }

    private ClassGroupEntity newGroup(CourseSubjectCompensationId id, UUID userId) {
        ClassGroupEntity g = new ClassGroupEntity();
        g.setCourseId(id.getCourseId());
        g.setSubjectId(id.getSubjectId());
        g.setStatus(STATUS_ACTIVE);
        g.setCreatedBy(userId);
        return g;
    }

    private static UUID resolveInstructorId(ClassGroupRequest in, UUID defaultInstructorId) {
        return in.getInstructorId() != null ? in.getInstructorId() : defaultInstructorId;
    }

    // ---------- schedule building ----------

    private List<ScheduleEntity> buildSchedules(
            CourseSubjectCompensationId id,
            List<ScheduleRequest> inputs,
            List<ScheduleEntity> existing,
            List<ClassGroupEntity> effectiveGroups) {

        List<ScheduleRequest> in = inputs == null ? List.of() : inputs;

        Set<UUID> updatedIds =
                in.stream().map(ScheduleRequest::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<ScheduleEntity> untouched =
                existing.stream().filter(s -> !updatedIds.contains(s.getId())).toList();

        scheduleValidator.validate(in, untouched, effectiveGroups, id.getCourseId(), id.getSubjectId());

        Map<UUID, ScheduleEntity> existingById =
                existing.stream().collect(Collectors.toMap(ScheduleEntity::getId, Function.identity()));
        UUID userId = currentUserId();

        return in.stream()
                .map(r -> {
                    ScheduleEntity s;
                    if (r.getId() == null) {
                        s = new ScheduleEntity();
                        s.setCourseId(id.getCourseId());
                        s.setSubjectId(id.getSubjectId());
                        s.setCreatedBy(userId);
                    } else {
                        s = existingById.get(r.getId());
                        if (s == null) {
                            throw new BusinessException(IAMErrorCode.SCHEDULE_NOT_FOUND);
                        }
                    }
                    s.setDayOfWeek(r.getDayOfWeek());
                    s.setStartedTime(r.getStartedTime());
                    s.setEndedTime(r.getEndedTime());
                    s.setDescription(r.getDescription());
                    return s;
                })
                .toList();
    }

    // ---------- response assembly ----------

    private CourseSubjectResponse toResponse(
            CourseSubjectEntity entity, List<ClassGroupEntity> groups, List<ScheduleEntity> schedules) {
        CourseSubjectResponse response = courseSubjectMapper.toResponse(entity);
        response.setClassGroups(toGroupResponses(groups));
        response.setSchedules(courseSubjectMapper.toScheduleResponses(schedules));
        return response;
    }

    private List<ClassGroupResponse> toGroupResponses(List<ClassGroupEntity> groups) {
        if (groups.isEmpty()) return List.of();

        Set<UUID> instructorIds = groups.stream()
                .map(ClassGroupEntity::getInstructorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, UserEntity> users = userRepository.findAllById(instructorIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return courseSubjectMapper.toGroupResponses(groups, users);
    }

    // ---------- lookups ----------

    private CourseEntity findCourse(UUID courseId) {
        return courseRepository
                .findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.COURSE_NOT_FOUND));
    }

    private CourseSubjectEntity findLink(UUID courseId, UUID subjectId) {
        findCourse(courseId);
        return courseSubjectRepository
                .findById(new CourseSubjectCompensationId(courseId, subjectId))
                .orElseThrow(() -> new BusinessException(IAMErrorCode.COURSE_SUBJECT_NOT_FOUND));
    }

    private UserEntity findInstructor(UUID instructorId) {
        if (instructorId == null) {
            return null;
        }
        return userRepository
                .findById(instructorId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.USER_NOT_FOUND));
    }

    private static String normalizeStatus(String status) {
        return (status == null || status.isBlank()) ? STATUS_ACTIVE : status.toLowerCase(Locale.ROOT);
    }

    private UUID currentUserId() {
        // TODO: resolve from the authenticated principal (JWT "sub")
        return UUID.fromString("1e0f83bb-7531-4585-be53-4188a7902c2e"); // temporary, testing only
    }
}
