package io.sala.krob_krong.media.service.impl;

import io.sala.krob_krong.common.errors.CommonErrorCode;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.utils.s3.*;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.media.dto.SchoolImageResponse;
import io.sala.krob_krong.media.repository.MediaAssetRepository;
import io.sala.krob_krong.media.service.*;
import io.sala.krob_krong.school.service.SchoolAccess;
import java.io.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

@Service
@RequiredArgsConstructor
public class SchoolImageServiceImpl implements SchoolImageService {
    private final SchoolAccess access;
    private final SchoolImageProcessor processor;
    private final SchoolImageStore store;
    private final S3Util s3;
    private final S3Client client;
    private final S3Properties props;
    private final MediaAssetRepository media;

    @Override
    public SchoolImageResponse upload(UUID schoolId, String purpose, MultipartFile file) {
        SchoolAccess.editable(access.read(schoolId, "school.profile:edit"));
        var image = processor.process(file);
        var upload = store.prepare(schoolId, purpose, image);
        try {
            var stored = s3.upload(upload.storageKey(), new ByteArrayInputStream(image.bytes()), image.mimeType());
            if (stored.size() != image.bytes().length)
                throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "Image storage verification failed");
            return store.complete(upload, image);
        } catch (RuntimeException ex) {
            try {
                s3.delete(upload.storageKey());
            } catch (RuntimeException cleanup) {
                ex.addSuppressed(cleanup);
            }
            try {
                store.fail(upload.id());
            } catch (RuntimeException cleanup) {
                ex.addSuppressed(cleanup);
            }
            throw ex;
        }
    }

    @Override
    public Download download(UUID schoolId, UUID assetId) {
        access.read(schoolId, "media.public:view");
        var asset = media.findById(assetId)
                .filter(a -> schoolId.equals(a.getSchoolId())
                        && a.isReady()
                        && Set.of("school_logo", "school_cover").contains(a.getPurpose()))
                .orElseThrow(() -> new BusinessException(IAMErrorCode.MEDIA_NOT_FOUND));
        try (var stream = client.getObject(r -> r.bucket(props.getBucket()).key(asset.getStorageKey()))) {
            byte[] bytes = stream.readNBytes(SchoolImageProcessor.MAX_STORED_BYTES + 1);
            if (bytes.length != asset.getByteSize()
                    || !SchoolImageProcessor.hash(bytes).equals(asset.getSha256()))
                throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "Stored image failed its integrity check");
            return new Download(bytes, asset.getMimeType());
        } catch (IOException ex) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "Image could not be read");
        }
    }
}
