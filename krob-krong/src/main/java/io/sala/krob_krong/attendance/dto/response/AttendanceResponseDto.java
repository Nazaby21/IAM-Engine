package io.sala.krob_krong.attendance.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class AttendanceResponseDto {
    @NotNull
    @JsonProperty("attendance_date")
    private LocalDate attendanceDate;

    @NotNull
    @JsonProperty("schedule_id")
    private UUID scheduleId;

    @NotEmpty
    @JsonProperty("records")
    private List<AttendanceRecord> records;

    @Data
    @Builder
    @AllArgsConstructor
    public static class AttendanceRecord {
        @NotNull
        @JsonProperty("student_id")
        private UUID studentId;

        @NotNull
        @JsonProperty("status")
        private String status;

        @JsonProperty("reason")
        private String reason;
    }
}
