package io.sala.krob_krong.announcement.service.impl;

import io.sala.krob_krong.announcement.constant.AnnounceStatus;
import io.sala.krob_krong.announcement.dto.request.AnnounceMakeRequest;
import io.sala.krob_krong.announcement.dto.response.AnnounceResponse;
import io.sala.krob_krong.announcement.entity.AnnouncementEntity;
import io.sala.krob_krong.announcement.mapper.AnnounceMapper;
import io.sala.krob_krong.announcement.repository.AnnounceRepository;
import io.sala.krob_krong.announcement.service.AnnounceService;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.event.dto.response.EventResponse;
import io.sala.krob_krong.event.mapper.EventMapper;
import io.sala.krob_krong.event.service.EventService;
import io.sala.krob_krong.event.service.validator.EventValidator;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.school.service.SchoolAccess;
import io.sala.krob_krong.utils.IdGenerator;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnnounceServiceImpl implements AnnounceService {
    private final AnnounceMapper announceMapper;
    private final AnnounceRepository announceRepository;
    private final IdGenerator idGenerator;
    private final EventService eventService;
    private final EventValidator eventValidator;
    private final EventMapper eventMapper;

    @Override
    public AnnounceResponse make(AnnounceMakeRequest request) {
        AnnouncementEntity announcement = announceMapper.toEntity(request);
        UUID actor = SchoolAccess.actor();
        announcement.setId(idGenerator.getAnnounceId());
        announcement.setCreatedBy(actor);
        announcement.setUpdatedBy(actor);
        Instant now = Instant.now();
        Instant scheduledAt = request.getScheduleAt();
        if (scheduledAt == null) {
            announcement.setStatus(AnnounceStatus.PUBLISHED.name());
            announcement.setPublishedAt(now);
        } else {
            if (!scheduledAt.isAfter(now)) {
                throw new BusinessException(IAMErrorCode.INVALID_SCHEDULED_DATE);
            }

            announcement.setStatus(AnnounceStatus.SCHEDULED.name());
            announcement.setScheduledAt(scheduledAt);
        }
        AnnouncementEntity save = announceRepository.save(announcement);
        AnnounceResponse response = announceMapper.toResponse(save);
        if (save.getEventId() != null) {
            EventResponse event = eventService.getById(save.getEventId());
            response.setEvent(event);
        }
        return response;
    }

    @Override
    public AnnounceResponse viewDetail(String id) {
        AnnouncementEntity announcement =
                announceRepository.findById(id).orElseThrow(() -> new BusinessException(IAMErrorCode.ANN_NOT_FOUND));
        AnnounceResponse response = announceMapper.toResponse(announcement);
        if (announcement.getEventId() != null) {
            EventResponse event = eventService.getById(announcement.getEventId());
            response.setEvent(event);
        }
        return response;
    }

    @Override
    public PageResponse<AnnounceResponse> view(PageRequest page, AnnounceStatus status) {
        Pageable pageable = page.toSpring(Sort.by(Sort.Direction.DESC, AnnouncementEntity.Fields.createdAt));
        Page<AnnouncementEntity> announcements = status == null
                ? announceRepository.findAll(pageable)
                : announceRepository.findAllByStatus(status.name(), pageable);
        return PageResponse.from(announcements.map(announcement -> {
            AnnounceResponse response = announceMapper.toResponse(announcement);
            if (announcement.getEventId() != null) {
                EventResponse event = eventService.getById(announcement.getEventId());
                response.setEvent(event);
            }
            return response;
        }));
    }
}