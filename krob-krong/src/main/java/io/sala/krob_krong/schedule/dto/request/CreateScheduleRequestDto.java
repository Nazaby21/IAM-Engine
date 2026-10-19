package io.sala.krob_krong.schedule.dto.request;

import io.sala.krob_krong.common.enumz.DayOfWeeks;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateScheduleRequestDto {
    private UUID classGroupId;
    private UUID classroomId;
    private List<DayOfWeeks> dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String description;

    @Data
    public static class DayOfWeeks{
        private DayOfWeeks dayOfWeek;
    }
}
