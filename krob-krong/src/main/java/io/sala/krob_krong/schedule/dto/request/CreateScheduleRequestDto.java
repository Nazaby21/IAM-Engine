package io.sala.krob_krong.schedule.dto.request;

import io.sala.krob_krong.common.enumz.DayOfWeeks;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateScheduleRequestDto {
    private UUID classGroupId;
    private String description;
    private List<data> schedule;

    @Data
    public static class data {
        private UUID classroomId;
        private DayOfWeeks dayOfWeek;
        private LocalTime startedTime;
        private LocalTime endedTime;
    }
}
