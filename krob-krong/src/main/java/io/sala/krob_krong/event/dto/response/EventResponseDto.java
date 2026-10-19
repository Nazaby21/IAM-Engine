package io.sala.krob_krong.event.dto.response;

import lombok.Data;
import org.springframework.core.SpringVersion;

import java.time.LocalDate;

@Data
public class EventResponseDto {
    private String id;
    private String schoolId;
    private String title;
    private String description;
    private LocalDate eventDate;
    private String status;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}
