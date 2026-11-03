package io.sala.krob_krong.schedule.service;

import io.sala.krob_krong.schedule.dto.request.CreateScheduleRequestDto;
import io.sala.krob_krong.schedule.dto.response.ScheduleResponseDto;

public interface ScheduleService {
    ScheduleResponseDto createSchedule(CreateScheduleRequestDto createScheduleRequestDto);
}
