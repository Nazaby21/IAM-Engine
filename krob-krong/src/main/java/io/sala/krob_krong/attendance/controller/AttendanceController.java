package io.sala.krob_krong.attendance.controller;

import io.sala.krob_krong.attendance.dto.request.AttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.response.AttendanceResponseDto;
import io.sala.krob_krong.attendance.mapper.AttendanceMapper;
import io.sala.krob_krong.attendance.repository.AttendanceRepository;
import io.sala.krob_krong.attendance.service.AttendanceService;
import io.sala.krob_krong.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;

    @PostMapping("/branch/{branchId}/schedule/{scheduleId}/attendance")
    public ApiResponse<List<AttendanceResponseDto>> attendanceRecords(
            @RequestParam UUID branchId,
            @RequestParam UUID scheduleId,
            @Valid @RequestBody AttendanceRequestDto request) {
        return ApiResponse.success(attendanceService.recordSessionAttendance(branchId, scheduleId, request));
    }
}
