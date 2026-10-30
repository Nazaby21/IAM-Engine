package io.sala.krob_krong.announcement.repository;

import io.sala.krob_krong.announcement.entity.AnnouncementEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnounceRepository extends JpaRepository<AnnouncementEntity, String> {
    List<AnnouncementEntity> findAllByStatusAndScheduledAtLessThanEqual(String status, Instant scheduledAt);

    Page<AnnouncementEntity> findAllByStatus(String status, Pageable pageable);
}
