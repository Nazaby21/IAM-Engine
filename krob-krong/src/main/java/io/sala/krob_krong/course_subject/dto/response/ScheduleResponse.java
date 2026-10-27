package io.sala.krob_krong.course_subject.dto.response;

import java.time.DayOfWeek;
import java.time.LocalTime;
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
@Builder
public class ScheduleResponse {
    private UUID id;
    private DayOfWeek dayOfWeek;
    private LocalTime startedTime;
    private LocalTime endedTime;
    private String description;
}
