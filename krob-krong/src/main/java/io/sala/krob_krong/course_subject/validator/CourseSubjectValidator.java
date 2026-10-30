package io.sala.krob_krong.course_subject.validator;

import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.classroom.entity.ClassroomEntity;
import io.sala.krob_krong.classroom.repository.ClassroomRepository;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.course.entity.CourseEntity;
import io.sala.krob_krong.course_subject.dto.request.ClassGroupRequest;
import io.sala.krob_krong.course_subject.dto.request.ScheduleRequest;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseSubjectValidator {

    private static final String STATUS_ACTIVE = "active";

    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;

    public void validateDates(Instant startedAt, Instant endedAt) {
        if (startedAt != null && endedAt != null && endedAt.isBefore(startedAt)) {
            throw new BusinessException(IAMErrorCode.COURSE_SUBJECT_INVALID_DATES);
        }
    }

    public void validateSchedules(CourseEntity course, List<ScheduleRequest> inputs, List<SchedulePlan> plans) {

        // schedule names unique in this course_subject
        Set<String> scheduleNames = new HashSet<>();
        for (SchedulePlan p : plans) {
            if (!scheduleNames.add(p.name().toLowerCase(Locale.ROOT))) {
                throw new BusinessException(IAMErrorCode.SCHEDULE_NAME_DUPLICATED);
            }
            // group names unique inside one schedule
            Set<String> groupNames = new HashSet<>();
            for (GroupPlan g : p.groups()) {
                if (!groupNames.add(g.name().toLowerCase(Locale.ROOT))) {
                    throw new BusinessException(IAMErrorCode.CLASS_GROUP_NAME_DUPLICATED);
                }
            }
            // the groups of a schedule must hold max_students
            if (p.touched()) {
                int total = p.groups().stream().mapToInt(GroupPlan::capacity).sum();
                if (total < p.maxStudents()) {
                    throw new BusinessException(IAMErrorCode.CLASS_GROUPS_CANNOT_HOLD_STUDENTS);
                }
            }
        }

        List<ClassGroupRequest> requestGroups = activeRequestGroups(inputs).toList();
        validateInstructorsExist(requestGroups);
        requestGroups.forEach(g -> validateClassroom(g, course));
    }

    private static Stream<ClassGroupRequest> activeRequestGroups(List<ScheduleRequest> inputs) {
        if (inputs == null) {
            return Stream.empty();
        }
        return inputs.stream()
                .filter(s -> isActive(s.getStatus()))
                .flatMap(s -> s.getClassGroups() == null ? Stream.empty() : s.getClassGroups().stream())
                .filter(g -> isActive(g.getStatus()));
    }

    // Only checks that the instructors exist. The same instructor may teach several groups (at different times).
    private void validateInstructorsExist(List<ClassGroupRequest> groups) {
        Set<UUID> ids = groups.stream()
                .map(ClassGroupRequest::getInstructorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!ids.isEmpty() && userRepository.findAllById(ids).size() != ids.size()) {
            throw new BusinessException(IAMErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateClassroom(ClassGroupRequest in, CourseEntity course) {
        if (in.getClassroomId() == null) {
            return;
        }
        ClassroomEntity room = classroomRepository
                .findById(in.getClassroomId())
                .filter(r -> STATUS_ACTIVE.equalsIgnoreCase(r.getStatus().toString()))
                .orElseThrow(() -> new BusinessException(IAMErrorCode.CLASSROOM_NOT_FOUND));

        if (!Objects.equals(room.getBranchId(), course.getBranchId())) {
            throw new BusinessException(IAMErrorCode.CLASSROOM_NOT_IN_COURSE_BRANCH);
        }
        if (room.getCapacity() != null && in.getCapacity() > room.getCapacity()) {
            throw new BusinessException(IAMErrorCode.CLASS_GROUP_EXCEEDS_ROOM_CAPACITY);
        }
    }

    private static boolean isActive(String status) {
        return status == null || STATUS_ACTIVE.equalsIgnoreCase(status);
    }
}