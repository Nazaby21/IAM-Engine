package io.sala.krob_krong.attendance.service.impl;

import io.sala.krob_krong.attendance.dto.request.AttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.response.AttendanceResponseDto;
import io.sala.krob_krong.attendance.entity.AttendanceEntity;
import io.sala.krob_krong.attendance.mapper.AttendanceMapper;
import io.sala.krob_krong.attendance.repository.AttendanceRepository;
import io.sala.krob_krong.attendance.service.AttendanceService;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceMapper attendanceMapper;
    private final AttendanceRepository attendanceRepository;

    @Override
    public List<AttendanceResponseDto> recordSessionAttendance(AttendanceRequestDto request) {
        attendanceRepository.findAllByStudentId()
                .stream()
                .orElseThrow(() -> new BusinessException(IAMErrorCode.STUDENT_NOT_FOUND));
        AttendanceEntity attendanceEntity = attendanceMapper.toAttendanceEntity(request);
        return attendanceMapper.toAttendanceResponse(attendanceEntity);
    }
}
