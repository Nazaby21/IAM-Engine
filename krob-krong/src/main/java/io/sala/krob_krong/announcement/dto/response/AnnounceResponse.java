package io.sala.krob_krong.announcement.dto.response;

import io.sala.krob_krong.event.dto.response.EventResponse;
import lombok.Data;

import java.util.UUID;

@Data
public class AnnounceResponse {
    private String id;
    private UUID schoolId;
    private UUID classGroupId;
    private String title;
    private String description;
    private String type;
    private String attachment;
    private EventResponse event;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}
