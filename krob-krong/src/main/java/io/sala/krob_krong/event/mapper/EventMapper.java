package io.sala.krob_krong.event.mapper;

import io.sala.krob_krong.event.dto.request.EventCreateRequestDto;
import io.sala.krob_krong.event.dto.response.CountdownResponse;
import io.sala.krob_krong.event.dto.response.EventResponseDto;
import io.sala.krob_krong.event.entity.EventEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventResponseDto toResponse(EventEntity eventEntity);

    EventEntity toEventEntity(EventCreateRequestDto request);

    CountdownResponse toCountdown(EventEntity eventEntity);
}
