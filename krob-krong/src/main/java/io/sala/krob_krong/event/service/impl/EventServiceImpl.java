package io.sala.krob_krong.event.service.impl;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.event.dto.request.EventCreateRequestDto;
import io.sala.krob_krong.event.dto.response.EventResponseDto;
import io.sala.krob_krong.event.entity.EventEntity;
import io.sala.krob_krong.event.mapper.EventMapper;
import io.sala.krob_krong.event.repository.EventRepository;
import io.sala.krob_krong.event.service.EventService;
import io.sala.krob_krong.event.service.validator.EventValidator;
import io.sala.krob_krong.event.specification.EventSpecz;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventValidator eventValidator;

    @Override
    public List<EventResponseDto> listAll() {
        return eventRepository.findAll().stream()
                .map(eventMapper::toResponse).toList();
    }

    @Override
    public EventResponseDto create(UUID schoolId, EventCreateRequestDto request) {
        LocalDate eventDate = request.getEventDate();
        eventValidator.validateEventDate(eventDate);
        Specification<EventEntity> specification =
                EventSpecz.bySchoolIdAndTitleAndEventDate(
                        schoolId,
                        request.getTitle(),
                        eventDate
                );
        eventValidator.NotExist(specification);
        EventEntity entity = eventMapper.toEventEntity(request);
        entity.setSchoolId(schoolId);
        EventEntity savedEntity = eventRepository.save(entity);

        return eventMapper.toResponse(savedEntity);
    }
}
