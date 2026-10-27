package io.sala.krob_krong.attendance.service;

import io.sala.krob_krong.attendance.dto.request.AttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.response.AttendanceResponseDto;

public interface AttendanceService {
    AttendanceResponseDto recordSessionAttendance(AttendanceRequestDto request);
}
