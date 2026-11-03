package io.sala.krob_krong.announcement.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
public class AnnounceMakeRequest {
    @JsonProperty("event_id")
    @NotNull(message = "event id is required for making announcement")
    private UUID eventId;

    @JsonProperty("school_id")
    @NotNull(message = "school id is require for making announcement")
    private UUID schoolId;

    @JsonProperty("class_group_id")
    @NotNull(message = "class group id is required")
    private UUID classGroupId;

    @JsonProperty("title")
    @NotBlank(message = "title is required")
    private String title;

    @JsonProperty("description")
    @NotBlank(message = "description is required")
    private String description;

    @JsonProperty("type")
    private String type;

    @JsonProperty("attachment")
    private String attachment;

    @JsonProperty("scheduled_at")
    private Instant scheduleAt;
}
