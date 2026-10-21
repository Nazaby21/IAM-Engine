package io.sala.krob_krong.event.service.impl;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.event.dto.request.EventCreateRequest;
import io.sala.krob_krong.event.dto.response.CountdownResponse;
import io.sala.krob_krong.event.dto.response.EventResponse;
import io.sala.krob_krong.event.entity.EventEntity;
import io.sala.krob_krong.event.mapper.EventMapper;
import io.sala.krob_krong.event.repository.EventRepository;
import io.sala.krob_krong.event.service.EventService;
import io.sala.krob_krong.event.service.validator.EventValidator;
import io.sala.krob_krong.event.specification.EventSpecz;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.school.service.SchoolAccess;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventValidator eventValidator;

    @Override
    public PageResponse<EventResponse> listAll(PageRequest page) {
        return PageResponse.from(eventRepository
                .findAll(page.toSpring(Sort.by(Sort.Direction.DESC, EventEntity.Fields.createdAt)))
                .map(eventMapper::toResponse));
    }

    @Override
    public EventResponse listById(UUID id) {
        EventEntity event =
                eventRepository.findById(id).orElseThrow(() -> new BusinessException(IAMErrorCode.EVENT_NOT_FOUND));
        return (eventMapper.toResponse(event));
    }

    @Override
    public EventResponse create(UUID schoolId, EventCreateRequest request) {
        UUID actor = SchoolAccess.actor();
        LocalDate eventDate = request.getEventDate();
        eventValidator.validateEventDate(eventDate);
        Specification<EventEntity> specification =
                EventSpecz.bySchoolIdAndTitleAndEventDate(schoolId, request.getTitle(), eventDate);
        eventValidator.NotExist(specification);
        EventEntity entity = eventMapper.toEventEntity(request);
        entity.setSchoolId(schoolId);
        entity.setCreatedBy(actor);
        EventEntity savedEntity = eventRepository.save(entity);

        return eventMapper.toResponse(savedEntity);
    }

    @Override
    public CountdownResponse getCountdown(UUID id) {
        EventEntity event =
                eventRepository.findById(id).orElseThrow(() -> new BusinessException(IAMErrorCode.EVENT_NOT_FOUND));
        CountdownResponse response = eventMapper.toCountdown(event);
        LocalDate today = LocalDate.now();
        LocalDate eventDate = event.getEventDate();
        if (ObjectUtils.isEmpty(eventDate)) {
            throw new BusinessException(IAMErrorCode.EVENT_DATE_REQUIRED);
        }
        long dayRemaining = ChronoUnit.DAYS.between(today, eventDate);
        response.setDaysRemaining(dayRemaining);

        return response;
    }
}
