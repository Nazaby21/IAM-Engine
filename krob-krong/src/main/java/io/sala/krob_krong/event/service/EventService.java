package io.sala.krob_krong.event.service;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.event.dto.request.EventCreateRequestDto;
import io.sala.krob_krong.event.dto.response.CountdownResponse;
import io.sala.krob_krong.event.dto.response.EventResponseDto;
import java.util.UUID;

public interface EventService {

    PageResponse<EventResponseDto> listAll(PageRequest page);

    EventResponseDto listById(UUID id);

    EventResponseDto create(UUID schoolId, EventCreateRequestDto request);

    CountdownResponse getCountdown(UUID id);
}
