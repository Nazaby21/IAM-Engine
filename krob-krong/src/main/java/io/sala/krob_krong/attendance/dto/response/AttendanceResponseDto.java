package io.sala.krob_krong.attendance.dto.response;

import io.sala.krob_krong.common.enumz.AttendanceStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceResponseDto {
    private UUID enrollmentId;
    private AttendanceStatus status;
    private String reason;
}
