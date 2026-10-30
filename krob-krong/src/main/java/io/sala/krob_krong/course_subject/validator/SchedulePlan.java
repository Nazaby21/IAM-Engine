package io.sala.krob_krong.course_subject.validator;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public record SchedulePlan(
        String name,
        Set<DayOfWeek> days,
        LocalTime start,
        LocalTime end,
        Integer maxStudents,
        List<GroupPlan> groups,
        boolean touched) {

    public boolean overlaps(SchedulePlan o) {
        return !Collections.disjoint(days, o.days) && start.isBefore(o.end) && end.isAfter(o.start);
    }
}