package io.sala.krob_krong.attendance.service;

import io.sala.krob_krong.attendance.dto.request.AttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.request.CreateAttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.response.AttendanceResponseDto;
import io.sala.krob_krong.attendance.dto.response.CreateAttendanceResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AttendanceService {
    CreateAttendanceResponseDto recordAttendance(CreateAttendanceRequestDto request);

    AttendanceResponseDto updateAttendance(AttendanceRequestDto request);

    List<AttendanceResponseDto> getAttendance(UUID studentId);

    Page<AttendanceResponseDto> searchAttendance(UUID search, Pageable pageable);
}
