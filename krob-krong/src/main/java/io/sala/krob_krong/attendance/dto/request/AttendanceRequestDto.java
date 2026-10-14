package io.sala.krob_krong.attendance.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.sala.krob_krong.common.enumz.AttendanceStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AttendanceRequestDto {
    @JsonProperty("records")
    private List<StudentRequestRecord> records;

    @Data
    public static class StudentRequestRecord {
        @JsonProperty("student_id")
        private UUID studentId;
        @JsonProperty("status")
        private AttendanceStatus status;
        @JsonProperty("reason")
        private String reason;
    }
}
