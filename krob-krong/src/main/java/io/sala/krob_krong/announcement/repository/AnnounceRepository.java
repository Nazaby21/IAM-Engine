package io.sala.krob_krong.announcement.repository;

import io.sala.krob_krong.announcement.entity.AnnouncementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnnounceRepository extends JpaRepository <AnnouncementEntity, String> {
}
