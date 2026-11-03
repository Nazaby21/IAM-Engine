package io.sala.krob_krong.announcement.service.impl;

import io.sala.krob_krong.announcement.constant.AnnounceStatus;
import io.sala.krob_krong.announcement.entity.AnnouncementEntity;
import io.sala.krob_krong.announcement.repository.AnnounceRepository;
import java.time.Instant;
import java.util.List;
import io.sala.krob_krong.announcement.service.Scheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchedulerImpl implements Scheduler {
    private final AnnounceRepository announceRepository;

    @Override
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void publishScheduledAnnouncements() {
        Instant now = Instant.now();
        List<AnnouncementEntity> announcements =
                announceRepository.findAllByStatusAndScheduledAtLessThanEqual(AnnounceStatus.SCHEDULED.name(), now);
        for (AnnouncementEntity announcement : announcements) {
            announcement.setStatus(AnnounceStatus.PUBLISHED.name());
            announcement.setPublishedAt(now);
        }
        announceRepository.saveAll(announcements);
    }
}