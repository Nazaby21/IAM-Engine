package io.sala.krob_krong.announcement.dto.response;

import io.sala.krob_krong.announcement.constant.AnnounceStatus;
import io.sala.krob_krong.event.dto.response.EventResponse;
import java.util.UUID;
import lombok.Data;

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
    private AnnounceStatus status;
    private String scheduledAt;
    private String publishedAt;
}
