package io.sala.krob_krong.announcement.mapper;

import io.sala.krob_krong.announcement.dto.request.AnnounceMakeRequest;
import io.sala.krob_krong.announcement.dto.response.AnnounceResponse;
import io.sala.krob_krong.announcement.entity.AnnouncementEntity;
import io.sala.krob_krong.event.dto.response.EventResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper (componentModel = "spring")
public interface AnnounceMapper {
    AnnounceResponse toResponse (AnnouncementEntity entity);
    AnnouncementEntity toEntity (AnnounceMakeRequest request);

    void updateEvent(
            @MappingTarget AnnounceResponse response,
            EventResponse event
    );
}
