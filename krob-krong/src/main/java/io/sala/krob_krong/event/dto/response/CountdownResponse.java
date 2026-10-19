package io.sala.krob_krong.event.dto.response;

import io.sala.krob_krong.event.constant.EventStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CountdownResponse {
    private UUID id;
    private String title;
    private LocalDate eventDate;
    private long daysRemaining;
    private EventStatus status;
}
