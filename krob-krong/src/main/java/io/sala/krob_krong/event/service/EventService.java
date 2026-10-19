package io.sala.krob_krong.event.service;

import io.sala.krob_krong.event.dto.request.EventCreateRequestDto;
import io.sala.krob_krong.event.dto.response.EventResponseDto;

import java.util.List;
import java.util.UUID;

public interface EventService {

    List<EventResponseDto> listAll();

    EventResponseDto create(UUID schoolId, EventCreateRequestDto request);
}
