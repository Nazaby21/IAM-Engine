package io.sala.krob_krong.schedule.controller;

import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.schedule.dto.request.CreateScheduleRequestDto;
import io.sala.krob_krong.schedule.dto.response.ScheduleResponseDto;
import io.sala.krob_krong.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedule")
public class ScheduleController {
    private final ScheduleService scheduleService;
    @PostMapping()
    public ApiResponse<ScheduleResponseDto> createSchedule(@RequestBody CreateScheduleRequestDto requestDto) {
        return ApiResponse.create(scheduleService.createSchedule(requestDto));
    }

}
