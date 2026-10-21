package io.sala.krob_krong.course_subject.validator;

import io.sala.krob_krong.attendance.entity.ScheduleEntity;
import io.sala.krob_krong.attendance.entity.repository.ScheduleRepository;
import io.sala.krob_krong.classroom.entity.ClassGroupEntity;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.course_subject.dto.request.ScheduleRequest;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleValidator {

    private record Slot(
            java.time.@jakarta.validation.constraints.NotNull DayOfWeek day, LocalTime start, LocalTime end) {}

    private final ScheduleRepository scheduleRepository;

    public void validate(
            List<ScheduleRequest> inputs,
            List<ScheduleEntity> untouched,
            List<ClassGroupEntity> groups,
            UUID courseId,
            UUID subjectId) {

        for (ScheduleRequest s : inputs) {
            if (!s.getEndedTime().isAfter(s.getStartedTime())) {
                throw new BusinessException(IAMErrorCode.SCHEDULE_INVALID_TIME);
            }
        }

        List<Slot> slots = Stream.concat(
                        untouched.stream().map(e -> new Slot(e.getDayOfWeek(), e.getStartedTime(), e.getEndedTime())),
                        inputs.stream().map(r -> new Slot(r.getDayOfWeek(), r.getStartedTime(), r.getEndedTime())))
                .toList();

        // 1. overlap inside this course_subject
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                if (overlaps(slots.get(i), slots.get(j))) {
                    throw new BusinessException(IAMErrorCode.SCHEDULE_OVERLAP);
                }
            }
        }

        // 2. rooms and instructors of ALL groups vs other course_subjects
        Set<UUID> rooms = groups.stream()
                .map(ClassGroupEntity::getClassroomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<UUID> instructors = groups.stream()
                .map(ClassGroupEntity::getInstructorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Slot s : slots) {
            if (!rooms.isEmpty()
                    && scheduleRepository.roomBusy(rooms, courseId, subjectId, s.day(), s.start(), s.end())) {
                throw new BusinessException(IAMErrorCode.CLASSROOM_BUSY);
            }
            if (!instructors.isEmpty()
                    && scheduleRepository.instructorBusy(
                            instructors, courseId, subjectId, s.day(), s.start(), s.end())) {
                throw new BusinessException(IAMErrorCode.INSTRUCTOR_BUSY);
            }
        }
    }

    private static boolean overlaps(Slot a, Slot b) {
        return a.day() == b.day() && a.start().isBefore(b.end()) && a.end().isAfter(b.start());
    }
}
