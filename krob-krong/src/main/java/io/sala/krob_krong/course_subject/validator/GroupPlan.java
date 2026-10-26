package io.sala.krob_krong.course_subject.validator;

import java.util.UUID;

public record GroupPlan(String name, UUID classroomId, UUID instructorId, int capacity) {}
