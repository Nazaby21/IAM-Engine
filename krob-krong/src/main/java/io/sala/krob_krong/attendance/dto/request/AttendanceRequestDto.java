package io.sala.krob_krong.attendance.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.sala.krob_krong.common.enumz.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceRequestDto {
    @NotNull(message = "Attendance date is require")
    @JsonProperty("attendance_date")
    private LocalDate attendanceDate;

    @NotNull(message = "Schedule id is require")
    @JsonProperty("schedule_id")
    private UUID scheduleId;

    @NotNull(message = "Student id is require")
    @JsonProperty("student_id")
    private UUID studentId;

    @NotNull(message = "Status is require")
    @JsonProperty("status")
    private AttendanceStatus status;

    @NotBlank(message = "Reason is require")
    @JsonProperty("reason")
    private String reason;
}
