package io.sala.krob_krong.attendance.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.sala.krob_krong.common.enumz.AttendanceStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class AttendanceResponseDto {
    @JsonProperty("attendance_date")
    private LocalDate attendanceDate;

    @JsonProperty("schedule_id")
    private UUID scheduleId;

    @JsonProperty("student_id")
    private UUID studentId;

    @JsonProperty("status")
    private AttendanceStatus status;

    @JsonProperty("reason")
    private String reason;
}
