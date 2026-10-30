package io.sala.krob_krong.schedule.dto.response;

import io.sala.krob_krong.common.enumz.DayOfWeeks;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ScheduleResponseDto {
    private UUID classGroupId;
    private String description;
    private List<data> schedule;

    @Data
    @Builder
    @AllArgsConstructor
    public static class data {
        private UUID classroomId;
        private DayOfWeeks dayOfWeek;
        private LocalTime startedTime;
        private LocalTime endedTime;
    }
}
