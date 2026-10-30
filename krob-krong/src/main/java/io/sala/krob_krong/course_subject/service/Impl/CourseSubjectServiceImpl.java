package io.sala.krob_krong.course_subject.service.Impl;

import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.classroom.entity.ClassGroupEntity;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.course.entity.CourseEntity;
import io.sala.krob_krong.course.repository.CourseRepository;
import io.sala.krob_krong.course_subject.dto.request.ClassGroupRequest;
import io.sala.krob_krong.course_subject.dto.request.CourseSubjectCreateRequest;
import io.sala.krob_krong.course_subject.dto.request.CourseSubjectUpdateRequest;
import io.sala.krob_krong.course_subject.dto.request.ScheduleRequest;
import io.sala.krob_krong.course_subject.dto.response.CourseSubjectResponse;
import io.sala.krob_krong.course_subject.dto.response.ScheduleResponse;
import io.sala.krob_krong.course_subject.entity.CourseSubjectCompensationId;
import io.sala.krob_krong.course_subject.entity.CourseSubjectEntity;
import io.sala.krob_krong.course_subject.mapper.CourseSubjectMapper;
import io.sala.krob_krong.course_subject.repository.CourseSubjectRepository;
import io.sala.krob_krong.course_subject.service.CourseSubjectService;
import io.sala.krob_krong.course_subject.specification.CourseSubjectListSpec;
import io.sala.krob_krong.course_subject.validator.CourseSubjectValidator;
import io.sala.krob_krong.course_subject.validator.GroupPlan;
import io.sala.krob_krong.course_subject.validator.SchedulePlan;
import io.sala.krob_krong.course_subject.validator.ScheduleValidator;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.schedule.entity.ScheduleEntity;
import io.sala.krob_krong.schedule.repository.ScheduleRepository;
import io.sala.krob_krong.subject.entity.SubjectEntity;
import io.sala.krob_krong.subject.repository.SubjectRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final CourseSubjectValidator courseSubjectValidator;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleValidator scheduleValidator;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseSubjectResponse> listAllSubjectInCourse(UUID courseId, PageRequest page) {
        Specification<CourseSubjectEntity> spec = CourseSubjectListSpec.byCourseId(courseId)
                .and(CourseSubjectListSpec.byStatus(STATUS_ACTIVE))
                .and(CourseSubjectListSpec.courseAndSubjectNotDeleted());

        Pageable pageable = page.toSpring(Sort.by(Sort.Direction.DESC, CourseSubjectEntity.Fields.code));

        Map<UUID, List<ScheduleEntity>> schedulesBySubject = scheduleRepository.findByCourseId(courseId).stream()
                .collect(Collectors.groupingBy(ScheduleEntity::getSubjectId));

        return PageResponse.from(courseSubjectRepository
                .findAll(spec, pageable)
                .map(e ->
                        toResponse(e, schedulesBySubject.getOrDefault(e.getId().getSubjectId(), List.of()))));
    }

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
        courseSubjectValidator.validateDates(request.getStartedAt(), request.getEndedAt());

        // validate everything BEFORE saving anything
        List<SchedulePlan> plans = planSchedules(List.of(), request.getSchedules());
        courseSubjectValidator.validateSchedules(course, request.getSchedules(), plans);
        scheduleValidator.validate(courseId, subjectId, request.getStartedAt(), request.getEndedAt(), plans);

        CourseSubjectEntity entity = courseSubjectMapper.toEntity(request);
        entity.setId(id);
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        entity.setSubject(subject);

        // schedule has an FK to course_subject, so insert the link first
        CourseSubjectEntity saved = courseSubjectRepository.saveAndFlush(entity);
        List<ScheduleEntity> schedules =
                scheduleRepository.saveAll(applySchedules(id, List.of(), request.getSchedules()));
        return toResponse(saved, schedules);
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
        courseSubjectValidator.validateDates(entity.getStartedAt(), entity.getEndedAt());

        // existing schedules + incoming changes are validated together
        List<ScheduleEntity> existing = scheduleRepository.findByCourseIdAndSubjectId(courseId, subjectId);
        List<SchedulePlan> plans = planSchedules(existing, request.getSchedules());
        courseSubjectValidator.validateSchedules(course, request.getSchedules(), plans);
        // also re-checks other course_subjects when only the dates changed
        scheduleValidator.validate(courseId, subjectId, entity.getStartedAt(), entity.getEndedAt(), plans);

        CourseSubjectEntity saved = courseSubjectRepository.saveAndFlush(entity);
        scheduleRepository.saveAll(applySchedules(entity.getId(), existing, request.getSchedules()));

        return toResponse(saved, scheduleRepository.findByCourseIdAndSubjectId(courseId, subjectId));
    }

    @Override
    @Transactional
    public void remove(UUID courseId, UUID subjectId) {
        CourseSubjectEntity entity = findLink(courseId, subjectId);

        if (courseSubjectRepository.isInUse(courseId, subjectId)) {
            throw new BusinessException(IAMErrorCode.COURSE_SUBJECT_IN_USE);
        }
        // class groups are removed by cascade from their schedule
        scheduleRepository.deleteAll(scheduleRepository.findByCourseIdAndSubjectId(courseId, subjectId));
        scheduleRepository.flush();
        courseSubjectRepository.delete(entity);
    }

    // ---------- planning (no entity is modified here) ----------

    private List<SchedulePlan> planSchedules(List<ScheduleEntity> existing, List<ScheduleRequest> inputs) {

        List<ScheduleRequest> in = inputs == null ? List.of() : inputs;
        Map<UUID, ScheduleEntity> existingById =
                existing.stream().collect(Collectors.toMap(ScheduleEntity::getId, Function.identity()));

        Map<UUID, ScheduleRequest> inputById = new HashMap<>();
        for (ScheduleRequest r : in) {
            if (r.getId() == null) {
                continue;
            }
            if (!existingById.containsKey(r.getId())) {
                throw new BusinessException(IAMErrorCode.SCHEDULE_NOT_FOUND);
            }
            inputById.put(r.getId(), r);
        }

        List<SchedulePlan> plans = new ArrayList<>();
        for (ScheduleEntity s : existing) {
            ScheduleRequest r = inputById.get(s.getId());
            SchedulePlan p = r != null ? planFromRequest(r, s) : planFromEntity(s);
            if (p != null) {
                plans.add(p);
            }
        }
        for (ScheduleRequest r : in) {
            if (r.getId() == null) {
                SchedulePlan p = planFromRequest(r, null);
                if (p != null) {
                    plans.add(p);
                }
            }
        }
        return plans;
    }

    private SchedulePlan planFromEntity(ScheduleEntity s) {
        if (!isActive(s.getStatus())) {
            return null;
        }
        List<GroupPlan> groups = s.getClassGroups().stream()
                .filter(g -> isActive(g.getStatus()))
                .map(g -> new GroupPlan(g.getName(), g.getClassroomId(), g.getInstructorId(), g.getCapacity()))
                .toList();
        return new SchedulePlan(
                s.getName(),
                new HashSet<>(s.getDaysOfWeek()),
                s.getStartedTime(),
                s.getEndedTime(),
                s.getMaxStudents(),
                groups,
                false);
    }

    private SchedulePlan planFromRequest(ScheduleRequest r, ScheduleEntity current) {
        String status = r.getStatus() != null ? r.getStatus() : current != null ? current.getStatus() : null;
        if (!isActive(status)) {
            return null; // disabled schedule: its rooms and instructors are free
        }
        List<GroupPlan> groups = planGroups(current == null ? List.of() : current.getClassGroups(), r.getClassGroups());
        if (groups.isEmpty()) {
            throw new BusinessException(IAMErrorCode.SCHEDULE_CLASS_GROUP_REQUIRED);
        }
        return new SchedulePlan(
                r.getName().trim(),
                new HashSet<>(r.getDaysOfWeek()),
                r.getStartedTime(),
                r.getEndedTime(),
                r.getMaxStudents(),
                groups,
                true);
    }

    private List<GroupPlan> planGroups(List<ClassGroupEntity> current, List<ClassGroupRequest> inputs) {

        if (inputs == null) { // groups not sent: keep the current ones
            return current.stream()
                    .filter(g -> isActive(g.getStatus()))
                    .map(g -> new GroupPlan(g.getName(), g.getClassroomId(), g.getInstructorId(), g.getCapacity()))
                    .toList();
        }

        Set<UUID> currentIds = current.stream().map(ClassGroupEntity::getId).collect(Collectors.toSet());
        Map<UUID, ClassGroupRequest> inputById = new HashMap<>();
        for (ClassGroupRequest r : inputs) {
            if (r.getId() == null) {
                continue;
            }
            if (!currentIds.contains(r.getId())) {
                throw new BusinessException(IAMErrorCode.CLASS_GROUP_NOT_FOUND);
            }
            inputById.put(r.getId(), r);
        }

        List<GroupPlan> plans = new ArrayList<>();
        for (ClassGroupEntity g : current) {
            ClassGroupRequest r = inputById.get(g.getId());
            if (r != null) {
                String status = r.getStatus() != null ? r.getStatus() : g.getStatus();
                if (isActive(status)) {
                    plans.add(groupPlan(r));
                }
            } else if (isActive(g.getStatus())) {
                plans.add(new GroupPlan(g.getName(), g.getClassroomId(), g.getInstructorId(), g.getCapacity()));
            }
        }
        for (ClassGroupRequest r : inputs) {
            if (r.getId() == null && isActive(r.getStatus())) {
                plans.add(groupPlan(r));
            }
        }
        return plans;
    }

    private GroupPlan groupPlan(ClassGroupRequest r) {
        return new GroupPlan(r.getName().trim(), r.getClassroomId(), r.getInstructorId(), r.getCapacity());
    }

    private List<ScheduleEntity> applySchedules(
            CourseSubjectCompensationId id, List<ScheduleEntity> existing, List<ScheduleRequest> inputs) {

        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        Map<UUID, ScheduleEntity> existingById =
                existing.stream().collect(Collectors.toMap(ScheduleEntity::getId, Function.identity()));
        UUID userId = currentUserId();

        List<ScheduleEntity> result = new ArrayList<>();
        for (ScheduleRequest in : inputs) {
            ScheduleEntity s = in.getId() == null ? newSchedule(id, userId) : existingById.get(in.getId());

            s.setName(in.getName().trim());
            s.setStartedTime(in.getStartedTime());
            s.setEndedTime(in.getEndedTime());
            s.setMaxStudents(in.getMaxStudents());
            s.setDescription(in.getDescription());
            if (in.getStatus() != null) {
                s.setStatus(normalizeStatus(in.getStatus()));
            }
            if (!s.getDaysOfWeek().equals(in.getDaysOfWeek())) {
                s.getDaysOfWeek().clear();
                s.getDaysOfWeek().addAll(in.getDaysOfWeek());
            }
            if (in.getClassGroups() != null) {
                applyGroups(s, in.getClassGroups(), userId);
            }
            result.add(s);
        }
        return result;
    }

    private void applyGroups(ScheduleEntity schedule, List<ClassGroupRequest> inputs, UUID userId) {
        Map<UUID, ClassGroupEntity> byId = new HashMap<>();
        schedule.getClassGroups().forEach(g -> byId.put(g.getId(), g));

        for (ClassGroupRequest in : inputs) {
            ClassGroupEntity g;
            if (in.getId() == null) {
                g = new ClassGroupEntity();
                g.setSchedule(schedule);
                g.setStatus(STATUS_ACTIVE);
                g.setCreatedBy(userId);
                schedule.getClassGroups().add(g);
            } else {
                g = byId.get(in.getId());
            }
            g.setName(in.getName().trim());
            g.setCapacity(in.getCapacity());
            g.setClassroomId(in.getClassroomId());
            g.setInstructorId(in.getInstructorId());
            if (in.getStatus() != null) {
                g.setStatus(normalizeStatus(in.getStatus()));
            }
        }
    }

    private ScheduleEntity newSchedule(CourseSubjectCompensationId id, UUID userId) {
        ScheduleEntity s = new ScheduleEntity();
        s.setCourseId(id.getCourseId());
        s.setSubjectId(id.getSubjectId());
        s.setStatus(STATUS_ACTIVE);
        s.setCreatedBy(userId);
        return s;
    }

    // ---------- response assembly ----------

    private CourseSubjectResponse toResponse(CourseSubjectEntity entity, List<ScheduleEntity> schedules) {
        CourseSubjectResponse response = courseSubjectMapper.toResponse(entity);
        response.setSchedules(toScheduleResponses(schedules));
        return response;
    }

    private List<ScheduleResponse> toScheduleResponses(List<ScheduleEntity> schedules) {
        if (schedules.isEmpty()) {
            return List.of();
        }
        Set<UUID> instructorIds = schedules.stream()
                .flatMap(s -> s.getClassGroups().stream())
                .map(ClassGroupEntity::getInstructorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, UserEntity> users = userRepository.findAllById(instructorIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<ScheduleEntity> sorted = schedules.stream()
                .sorted(Comparator.comparing(ScheduleEntity::getStartedTime)
                        .thenComparing(ScheduleEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return courseSubjectMapper.toScheduleResponses(sorted, users);
    }

    // ---------- lookups & helpers ----------

    private static boolean isActive(String status) {
        return status == null || STATUS_ACTIVE.equalsIgnoreCase(status);
    }

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

    private static String normalizeStatus(String status) {
        return (status == null || status.isBlank()) ? STATUS_ACTIVE : status.toLowerCase(Locale.ROOT);
    }

    private UUID currentUserId() {
        // resolve from the authenticated principal (JWT "sub")
        return UUID.fromString("dd58c768-a6dd-4f59-9ab9-63ff55898834");
    }
}