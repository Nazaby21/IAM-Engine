package io.sala.krob_krong.course_subject.validator;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.schedule.repository.ScheduleRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleValidator {

    private static final Instant MIN_DATE = Instant.parse("1900-01-01T00:00:00Z");
    private static final Instant MAX_DATE = Instant.parse("9999-12-31T23:59:59Z");

    private final ScheduleRepository scheduleRepository;

    public void validate(UUID courseId, UUID subjectId, Instant startedAt, Instant endedAt, List<SchedulePlan> plans) {

        // valid time range
        for (SchedulePlan p : plans) {
            if (!p.end().isAfter(p.start())) {
                throw new BusinessException(IAMErrorCode.SCHEDULE_INVALID_TIME);
            }
        }

        // inside this course_subject
        for (int i = 0; i < plans.size(); i++) {
            SchedulePlan a = plans.get(i);

            // groups of the SAME schedule meet at the same time: room and instructor must all differ
            for (int x = 0; x < a.groups().size(); x++) {
                for (int y = x + 1; y < a.groups().size(); y++) {
                    checkPair(a.groups().get(x), a.groups().get(y));
                }
            }
            // different schedules: only a conflict when their days and times overlap
            for (int j = i + 1; j < plans.size(); j++) {
                SchedulePlan b = plans.get(j);
                if (!a.overlaps(b)) {
                    continue;
                }
                for (GroupPlan x : a.groups()) {
                    for (GroupPlan y : b.groups()) {
                        checkPair(x, y);
                    }
                }
            }
        }

        // against OTHER active course_subjects (DB), only where the date ranges overlap
        Instant from = startedAt != null ? startedAt : MIN_DATE;
        Instant to = endedAt != null ? endedAt : MAX_DATE;

        for (SchedulePlan p : plans) {
            for (GroupPlan g : p.groups()) {
                String[] days = p.days().stream().map(Enum::name).toArray(String[]::new);
                if (g.classroomId() != null
                        && scheduleRepository.roomBusy(
                                g.classroomId(), courseId, subjectId, days.toString(), p.start(), p.end(), from, to)) {
                    throw new BusinessException(IAMErrorCode.CLASSROOM_BUSY);
                }
                if (g.instructorId() != null
                        && scheduleRepository.instructorBusy(
                                g.instructorId(), courseId, subjectId, days, p.start(), p.end(), from, to)) {
                    throw new BusinessException(IAMErrorCode.INSTRUCTOR_BUSY);
                }
            }
        }
    }

    private static void checkPair(GroupPlan a, GroupPlan b) {
        if (a.classroomId() != null && a.classroomId().equals(b.classroomId())) {
            throw new BusinessException(IAMErrorCode.CLASSROOM_BUSY);
        }
        if (a.instructorId() != null && a.instructorId().equals(b.instructorId())) {
            throw new BusinessException(IAMErrorCode.INSTRUCTOR_BUSY);
        }
    }
}
