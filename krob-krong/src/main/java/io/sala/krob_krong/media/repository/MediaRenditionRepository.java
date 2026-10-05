package io.sala.krob_krong.media.repository;

import io.sala.krob_krong.media.entity.MediaRenditionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MediaRenditionRepository
        extends JpaRepository<MediaRenditionEntity, UUID>, JpaSpecificationExecutor<MediaRenditionEntity> {}
