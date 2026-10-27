package io.sala.krob_krong.event.dto.response;

import java.time.LocalDate;
import lombok.Data;

@Data
public class EventResponse {
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
