package io.sala.krob_krong.event.mapper;

import io.sala.krob_krong.event.dto.request.EventCreateRequest;
import io.sala.krob_krong.event.dto.response.CountdownResponse;
import io.sala.krob_krong.event.dto.response.EventResponse;
import io.sala.krob_krong.event.entity.EventEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventResponse toResponse(EventEntity eventEntity);

    EventEntity toEventEntity(EventCreateRequest request);

    CountdownResponse toCountdown(EventEntity eventEntity);
}
