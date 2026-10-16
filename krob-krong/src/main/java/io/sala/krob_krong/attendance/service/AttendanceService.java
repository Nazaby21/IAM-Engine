package io.sala.krob_krong.attendance.service;

import io.sala.krob_krong.attendance.dto.request.AttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.response.AttendanceResponseDto;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {
    List<AttendanceResponseDto> recordSessionAttendance(UUID branchId, UUID scheduleId, AttendanceRequestDto request);
}
