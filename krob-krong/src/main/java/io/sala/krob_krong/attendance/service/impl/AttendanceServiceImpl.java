package io.sala.krob_krong.attendance.service.impl;

import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.attendance.dto.request.AttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.request.CreateAttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.response.AttendanceResponseDto;
import io.sala.krob_krong.attendance.dto.response.CreateAttendanceResponseDto;
import io.sala.krob_krong.attendance.entity.AttendanceEntity;
import io.sala.krob_krong.attendance.mapper.AttendanceMapper;
import io.sala.krob_krong.attendance.repository.AttendanceRepository;
import io.sala.krob_krong.attendance.service.AttendanceService;
import io.sala.krob_krong.attendance.specification.AttendanceSpecification;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.schedule.repository.ScheduleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final AttendanceMapper attendanceMapper;
    UUID userId = currentUserId();

    @Override
    public CreateAttendanceResponseDto recordAttendance(CreateAttendanceRequestDto request) {
        UUID scheduleId = request.getScheduleId();
        scheduleRepository
                .findById(scheduleId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.SCHEDULE_NOT_FOUND));
        List<AttendanceEntity> entities = new ArrayList<>();

        for (var item : request.getRecords()) {
            if (!userRepository.existsByStudentIdAndScheduleId(item.getStudentId(), request.getScheduleId())) {
                throw new BusinessException(IAMErrorCode.STUDENT_NOT_FOUND);
            }
            if (attendanceRepository.existsAttendance(
                    request.getAttendanceDate(), item.getStudentId(), request.getScheduleId())) {
                throw new BusinessException(IAMErrorCode.STUDENT_ALREADY_MARKED);
            }

            AttendanceEntity attendance = AttendanceEntity.builder()
                    .scheduleId(scheduleId)
                    .studentId(item.getStudentId())
                    .status(item.getStatus().toString())
                    .reason(item.getReason())
                    .attendanceDate(request.getAttendanceDate())
                    .createdBy(userId)
                    .build();

            entities.add(attendance);
        }

        List<AttendanceEntity> attendanceEntities = attendanceRepository.saveAll(entities);

        return CreateAttendanceResponseDto.builder()
                .attendanceDate(request.getAttendanceDate())
                .scheduleId(scheduleId)
                .records(attendanceEntities.stream()
                        .map(entity -> CreateAttendanceResponseDto.AttendanceRecord.builder()
                                .studentId(entity.getStudentId())
                                .status(entity.getStatus())
                                .reason(entity.getReason())
                                .build())
                        .toList())
                .build();
    }

    @Override
    public AttendanceResponseDto updateAttendance(AttendanceRequestDto request) {
        AttendanceEntity attendanceEntity = attendanceRepository
                .findAttendance(request.getAttendanceDate(), request.getStudentId(), request.getScheduleId())
                .orElseThrow(() -> new BusinessException(IAMErrorCode.STUDENT_NOT_FOUND));
        attendanceEntity.setStatus(request.getStatus().toString());
        attendanceEntity.setReason(request.getReason());
        attendanceEntity.setUpdatedBy(userId);
        attendanceRepository.save(attendanceEntity);
        return attendanceMapper.toResponse(attendanceEntity);
    }

    @Override
    public List<AttendanceResponseDto> getAttendance(UUID studentId) {
        List<AttendanceEntity> attendanceEntity = attendanceRepository.findByStudentId(studentId);
        return attendanceMapper.toResponses(attendanceEntity);
    }

    @Override
    public Page<AttendanceResponseDto> searchAttendance(UUID search, Pageable pageable) {
        Specification<AttendanceEntity> spac = AttendanceSpecification.build(search);
        Page<AttendanceEntity> attendance = attendanceRepository.findAll(spac, pageable);
        return attendance.map(attendanceMapper::toResponse);
    }

    private UUID currentUserId() {
        return UUID.fromString("017bbe3e-424a-4bda-8b5c-4e2b090d4712");
    }
}
