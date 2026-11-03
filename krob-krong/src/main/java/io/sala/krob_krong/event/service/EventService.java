package io.sala.krob_krong.event.service;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.event.dto.request.EventCreateRequest;
import io.sala.krob_krong.event.dto.response.CountdownResponse;
import io.sala.krob_krong.event.dto.response.EventResponse;
import java.util.UUID;

public interface EventService {

    PageResponse<EventResponse> listAll(PageRequest page);

    EventResponse listById(UUID id);

    EventResponse create(UUID schoolId, EventCreateRequest request);

    CountdownResponse getCountdown(UUID id);

    EventResponse getById(UUID ID);
}