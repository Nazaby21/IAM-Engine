package io.sala.krob_krong.event.mapper;

import io.sala.krob_krong.event.dto.request.EventCreateRequest;
import io.sala.krob_krong.event.dto.response.CountdownResponse;
import io.sala.krob_krong.event.dto.response.EventResponse;
import io.sala.krob_krong.event.entity.EventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventMapper {

    EventResponse toResponse(EventEntity eventEntity);

    EventEntity toEventEntity(EventCreateRequest request);

    CountdownResponse toCountdown(EventEntity eventEntity);
}
