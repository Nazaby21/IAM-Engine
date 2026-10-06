package io.sala.krob_krong.media.service;

import io.sala.krob_krong.media.dto.SchoolImageResponse;
import java.util.UUID;

public interface SchoolImageStore {
    record Upload(UUID id, UUID schoolId, String storageKey) {}

    Upload prepare(UUID schoolId, String purpose, SchoolImageProcessor.Image image);

    SchoolImageResponse complete(Upload upload, SchoolImageProcessor.Image image);

    void fail(UUID assetId);
}
