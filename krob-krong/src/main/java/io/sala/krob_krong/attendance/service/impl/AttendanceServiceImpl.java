package io.sala.krob_krong.attendance.service.impl;

import io.sala.krob_krong.attendance.dto.request.AttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.response.AttendanceResponseDto;
import io.sala.krob_krong.attendance.entity.AttendanceEntity;
import io.sala.krob_krong.attendance.mapper.AttendanceMapper;
import io.sala.krob_krong.attendance.repository.AttendanceRepository;
import io.sala.krob_krong.attendance.service.AttendanceService;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.enrollment.entity.EnrollmentEntity;
import io.sala.krob_krong.enrollment.repository.EnrollmentRepository;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceMapper attendanceMapper;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    public List<AttendanceResponseDto> recordSessionAttendance(
            UUID branchId, UUID scheduleId, AttendanceRequestDto request) {
        List<AttendanceRequestDto.StudentRequestRecord> records = request.getRecords();

        List<UUID> studentIds = request.getRecords().stream()
                .map(AttendanceRequestDto.StudentRequestRecord::getStudentId)
                .toList();
        if (studentIds.stream().distinct().count() != records.size()) {
            throw new BusinessException(IAMErrorCode.DUPLICATE_STUDENT_ID);
        }

        List<EnrollmentEntity> enrollments = enrollmentRepository.findAllEnrollmentByScheduleId(scheduleId, studentIds);

        Map<UUID, EnrollmentEntity> enrollmentMap =
                enrollments.stream().collect(Collectors.toMap(EnrollmentEntity::getUserId, Function.identity()));

        List<UUID> enrollmentIds =
                enrollments.stream().map(EnrollmentEntity::getId).toList();

        if (attendanceRepository.existsAttendance(scheduleId, enrollmentIds, request.getAttendanceDate())) {
            throw new BusinessException(IAMErrorCode.DUPLICATE_ATTENDANCE);
        }

        List<AttendanceEntity> attendanceEntities = records.stream()
                .map(record -> {
                    EnrollmentEntity enrollmentEntity = enrollmentMap.get(record.getStudentId());
                    AttendanceEntity attendanceEntity = AttendanceEntity.builder()
                            .attendanceDate(request.getAttendanceDate())
                            .scheduleId(scheduleId)
                            .enrollmentId(enrollmentEntity.getId())
                            .status(record.getStatus().name())
                            .reason(record.getReason())
                            .build();

                    return attendanceEntity;
                })
                .toList();
        List<AttendanceEntity> saved = attendanceRepository.saveAll(attendanceEntities);

        return saved.stream().map(attendanceMapper::toAttendanceResponse).toList();
    }
}
