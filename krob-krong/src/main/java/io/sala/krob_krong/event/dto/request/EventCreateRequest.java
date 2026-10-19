package io.sala.krob_krong.event.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.sala.krob_krong.event.constant.EventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class EventCreateRequest {

    @JsonProperty("title")
    @NotBlank(message = "event title cannot be blanked")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("event_date")
    @NotNull(message = "event date cannot be blank")
    private LocalDate eventDate;

    @JsonProperty("poster_image")
    private String posterImage;

    @NotNull(message = "status is required")
    private EventStatus status;
}
