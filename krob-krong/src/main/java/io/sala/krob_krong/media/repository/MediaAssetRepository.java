package io.sala.krob_krong.media.repository;

import io.sala.krob_krong.media.entity.MediaAssetEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MediaAssetRepository
        extends JpaRepository<MediaAssetEntity, UUID>, JpaSpecificationExecutor<MediaAssetEntity> {}
