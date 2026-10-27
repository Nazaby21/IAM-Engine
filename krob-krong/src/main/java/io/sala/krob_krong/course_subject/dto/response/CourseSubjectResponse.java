package io.sala.krob_krong.course_subject.dto.response;

import io.sala.krob_krong.subject.dto.SubjectResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CourseSubjectResponse {
    private UUID courseId;
    private SubjectResponse subject;
    private InstructorResponse instructor;
    private String code;
    private BigDecimal price;
    private String priceType;
    private String description;
    private Instant startedAt;
    private Instant endedAt;
    private String currency;
    private String status;
    private Boolean isPublic;
    private Integer maxStudents;
    private List<ClassGroupResponse> classGroups;
    private List<ScheduleResponse> schedules;
}
