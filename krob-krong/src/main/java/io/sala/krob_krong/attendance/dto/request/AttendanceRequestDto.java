package io.sala.krob_krong.attendance.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.sala.krob_krong.common.enumz.AttendanceStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceRequestDto {

    @NotNull
    @JsonProperty("attendance_date")
    private LocalDate attendanceDate;

    @NotNull
    @JsonProperty("schedule_id")
    private UUID scheduleId;

    @NotEmpty
    @JsonProperty("records")
    private List<StudentRequestRecord> records;

    @Data
    public static class StudentRequestRecord {
        @NotNull
        @JsonProperty("student_id")
        private UUID studentId;

        @NotNull
        @JsonProperty("status")
        private AttendanceStatus status;

        @JsonProperty("reason")
        private String reason;
    }
}
