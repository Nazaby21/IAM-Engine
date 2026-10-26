package io.sala.krob_krong.attendance.service.impl;

import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.attendance.dto.request.AttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.response.AttendanceResponseDto;
import io.sala.krob_krong.attendance.entity.AttendanceEntity;
import io.sala.krob_krong.attendance.mapper.AttendanceMapper;
import io.sala.krob_krong.attendance.repository.AttendanceRepository;
import io.sala.krob_krong.attendance.service.AttendanceService;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.enrollment.repository.EnrollmentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceMapper attendanceMapper;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    public AttendanceResponseDto recordSessionAttendance(AttendanceRequestDto request) {
        UUID scheduleId = request.getScheduleId();
        scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.SCHEDULE_NOT_FOUND));
        List<AttendanceEntity> entities = new ArrayList<>();

        for (var item: request.getRecords()){
            if (!userRepository.existsByStudentId(item.getStudentId(), request.getScheduleId())){
                throw new BusinessException(IAMErrorCode.STUDENT_NOT_FOUND);
            }
            if (attendanceRepository.existsAttendance(request.getAttendanceDate(), item.getStudentId(), request.getScheduleId())){
                throw new BusinessException(IAMErrorCode.STUDENT_ALREADY_MARKED);
            }

            AttendanceEntity attendance = AttendanceEntity.builder()
                    .scheduleId(scheduleId)
                    .studentId(item.getStudentId())
                    .status(item.getStatus().toString())
                    .reason(item.getReason())
                    .attendanceDate(request.getAttendanceDate())
                    .createdBy(UUID.fromString("f6ad7847-9f47-47b9-b7c4-5b3c26dd0255"))
                    .build();

            entities.add(attendance);
        }

        List<AttendanceEntity> attendanceEntities = attendanceRepository.saveAll(entities);

        return AttendanceResponseDto.builder()
                .attendanceDate(request.getAttendanceDate())
                .scheduleId(scheduleId)
                .records(attendanceEntities.stream()
                        .map(entity -> AttendanceResponseDto.AttendanceRecord
                                .builder()
                                .studentId(entity.getStudentId())
                                .status(entity.getStatus())
                                .reason(entity.getReason())
                                .build()
                        )
                        .toList()

                )
                .build();
    }
}
