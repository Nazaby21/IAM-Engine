package io.sala.krob_krong.media.service.impl;

import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.media.dto.SchoolImageResponse;
import io.sala.krob_krong.media.entity.MediaAssetEntity;
import io.sala.krob_krong.media.repository.MediaAssetRepository;
import io.sala.krob_krong.media.service.*;
import io.sala.krob_krong.school.repository.SchoolRepository;
import io.sala.krob_krong.school.service.SchoolAccess;
import jakarta.persistence.EntityManager;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchoolImageStoreImpl implements SchoolImageStore {
    private static final UUID PIPELINE_ID = UUID.fromString("f8a0dd81-a185-4ddb-916b-65b6f120c391");
    private final SchoolAccess access;
    private final MediaAssetRepository media;
    private final SchoolRepository schools;
    private final UserRepository users;
    private final EntityManager em;

    @Override
    @Transactional
    public Upload prepare(UUID schoolId, String purpose, SchoolImageProcessor.Image image) {
        var school = access.lock(schoolId, "school.profile:edit");
        SchoolAccess.editable(school);
        if (!Set.of("school_logo", "school_cover").contains(purpose))
            throw new BusinessException(IAMErrorCode.INVALID_IMAGE);
        var asset = new MediaAssetEntity();
        asset.setSchoolId(schoolId);
        asset.setUploadedBy(SchoolAccess.actor());
        asset.setKind("image");
        asset.setPurpose(purpose);
        asset.setVisibility("public");
        asset.setMimeType(image.mimeType());
        asset.setByteSize(image.bytes().length);
        asset.setStorageKey("pending/" + UUID.randomUUID());
        media.saveAndFlush(asset);
        em.refresh(asset);
        asset.setStatus("processing");
        media.saveAndFlush(asset);
        return new Upload(asset.getId(), schoolId, asset.getStorageKey());
    }

    @Override
    @Transactional
    public SchoolImageResponse complete(Upload upload, SchoolImageProcessor.Image image) {
        var school = access.lock(upload.schoolId(), "school.profile:edit");
        SchoolAccess.editable(school);
        var asset = media.findById(upload.id())
                .filter(a -> upload.schoolId().equals(a.getSchoolId())
                        && SchoolAccess.actor().equals(a.getUploadedBy())
                        && "processing".equals(a.getStatus()))
                .orElseThrow(() -> new BusinessException(IAMErrorCode.MEDIA_NOT_READY));
        UUID actor = SchoolAccess.actor();
        bindPipeline();
        try {
            asset.setWidth(image.width());
            asset.setHeight(image.height());
            asset.setSha256(image.sha256());
            asset.setStatus("ready");
            media.saveAndFlush(asset);
            em.refresh(asset);
        } finally {
            bindActor(actor);
        }
        if ("school_logo".equals(asset.getPurpose())) school.setLogoAssetId(asset.getId());
        else school.setCoverAssetId(asset.getId());
        schools.saveAndFlush(school);
        return SchoolImageResponse.builder()
                .id(asset.getId())
                .schoolId(asset.getSchoolId())
                .purpose(asset.getPurpose())
                .status(asset.getStatus())
                .mimeType(asset.getMimeType())
                .byteSize(asset.getByteSize())
                .width(asset.getWidth())
                .height(asset.getHeight())
                .sha256(asset.getSha256())
                .contentUrl("/api/schools/" + asset.getSchoolId() + "/media/" + asset.getId() + "/content")
                .build();
    }

    @Override
    @Transactional
    public void fail(UUID id) {
        var asset = media.findById(id).orElse(null);
        if (asset == null || !"processing".equals(asset.getStatus())) return;
        UUID actor = SchoolAccess.actor();
        bindPipeline();
        try {
            asset.setStatus("failed");
            media.saveAndFlush(asset);
        } finally {
            bindActor(actor);
        }
    }

    private void bindPipeline() {
        if (users.findById(PIPELINE_ID)
                .filter(u -> u.isService() && u.isActive())
                .isEmpty()) throw new IllegalStateException("School image processing identity is missing or inactive");
        bindActor(PIPELINE_ID);
    }

    private void bindActor(UUID id) {
        em.createNativeQuery("SELECT set_config('app.actor_id', :actor, true)")
                .setParameter("actor", id.toString())
                .getSingleResult();
    }
}
