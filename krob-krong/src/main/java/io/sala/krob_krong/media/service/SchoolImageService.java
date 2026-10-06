package io.sala.krob_krong.media.service;

import io.sala.krob_krong.media.dto.SchoolImageResponse;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface SchoolImageService {
    record Download(byte[] bytes, String mimeType) {}

    SchoolImageResponse upload(UUID schoolId, String purpose, MultipartFile file);

    Download download(UUID schoolId, UUID assetId);
}
