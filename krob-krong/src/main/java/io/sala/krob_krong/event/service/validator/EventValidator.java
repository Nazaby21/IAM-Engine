package io.sala.krob_krong.event.service.validator;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.event.entity.EventEntity;
import io.sala.krob_krong.event.repository.EventRepository;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventValidator {

    private final EventRepository eventRepository;

    public void validateEventDate(LocalDate eventDate) {
        if (eventDate.isBefore(LocalDate.now())) {
            throw new BusinessException(IAMErrorCode.EVENT_DATE_CANNOT_BE_PAST);
        }
    }

    public void NotExist(Specification<EventEntity> specification) {

        if (eventRepository.count(specification) > 0) {
            throw new BusinessException(IAMErrorCode.EVENT_ALREADY_EXIST);
        }
    }

    public EventEntity findById(UUID id) {
        return eventRepository.findById(id).orElseThrow(() -> new BusinessException(IAMErrorCode.EVENT_NOT_FOUND));
    }
}