package io.sala.krob_krong.attendance.dto.response;

import io.sala.krob_krong.common.enumz.AttendanceStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class AttendanceResponseDto {
    private UUID studentId;
    private AttendanceStatus status;
    private LocalDate checkInDate;
    private String reason;
}
