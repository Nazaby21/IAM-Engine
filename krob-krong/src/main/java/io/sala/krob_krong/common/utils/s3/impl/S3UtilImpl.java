package io.sala.krob_krong.common.utils.s3.impl;

import io.sala.krob_krong.common.errors.CommonErrorCode;
import io.sala.krob_krong.common.exceptions.KrobKrongException;
import io.sala.krob_krong.common.utils.s3.ETags;
import io.sala.krob_krong.common.utils.s3.S3Properties;
import io.sala.krob_krong.common.utils.s3.S3Util;
import io.sala.krob_krong.common.utils.s3.dto.MultipartUpload;
import io.sala.krob_krong.common.utils.s3.dto.PartETag;
import io.sala.krob_krong.common.utils.s3.dto.PresignedPart;
import io.sala.krob_krong.common.utils.s3.dto.PresignedUrl;
import io.sala.krob_krong.common.utils.s3.dto.StoredObject;
import io.sala.krob_krong.common.utils.s3.dto.UploadedPart;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3UtilImpl implements S3Util {

    private static final int MAX_PARTS = 10_000;

    private final S3Client s3;
    private final S3Presigner presigner;
    private final S3Properties props;

    @Override
    public PresignedUrl presignUpload(String key, String contentType) {
        return toPresignedUrl(presigner.presignPutObject(request -> request.signatureDuration(props.getPresignTtl())
                .putObjectRequest(put -> put.bucket(props.getBucket()).key(key).contentType(contentType))));
    }

    @Override
    public PresignedUrl presignDownload(String key) {
        return toPresignedUrl(presigner.presignGetObject(request -> request.signatureDuration(props.getPresignTtl())
                .getObjectRequest(get -> get.bucket(props.getBucket()).key(key))));
    }

    @Override
    public MultipartUpload startMultipartUpload(String key, String contentType) {
        String uploadId = s3.createMultipartUpload(
                        request -> request.bucket(props.getBucket()).key(key).contentType(contentType))
                .uploadId();
        return new MultipartUpload(key, uploadId, props.getPartSize().toBytes());
    }

    @Override
    public List<PresignedPart> presignUploadParts(String key, String uploadId, int partCount) {
        if (partCount < 1 || partCount > MAX_PARTS) {
            throw new KrobKrongException(CommonErrorCode.BAD_REQUEST, "partCount must be between 1 and " + MAX_PARTS);
        }
        return IntStream.rangeClosed(1, partCount)
                .mapToObj(partNumber -> {
                    PresignedUrl url = toPresignedUrl(
                            presigner.presignUploadPart(request -> request.signatureDuration(props.getPresignTtl())
                                    .uploadPartRequest(part -> part.bucket(props.getBucket())
                                            .key(key)
                                            .uploadId(uploadId)
                                            .partNumber(partNumber))));
                    return new PresignedPart(partNumber, url.url(), url.method(), url.headers(), url.expiresAt());
                })
                .toList();
    }

    @Override
    public List<UploadedPart> listUploadedParts(String key, String uploadId) {
        return s3
                .listPartsPaginator(
                        request -> request.bucket(props.getBucket()).key(key).uploadId(uploadId))
                .parts()
                .stream()
                .map(part -> new UploadedPart(part.partNumber(), ETags.unquote(part.eTag()), part.size()))
                .toList();
    }

    @Override
    public StoredObject completeMultipartUpload(String key, String uploadId, List<PartETag> parts) {
        if (parts == null || parts.isEmpty()) {
            throw new KrobKrongException(CommonErrorCode.BAD_REQUEST, "At least one uploaded part is required");
        }
        if (parts.stream().anyMatch(part -> part.eTag() == null || part.eTag().isBlank())) {
            throw new KrobKrongException(
                    CommonErrorCode.BAD_REQUEST, "Every part needs the ETag returned by its upload");
        }
        List<CompletedPart> completed = parts.stream()
                .sorted(Comparator.comparingInt(PartETag::partNumber))
                .map(part -> CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(ETags.quote(part.eTag()))
                        .build())
                .toList();
        if (completed.stream().map(CompletedPart::partNumber).distinct().count() != completed.size()) {
            throw new KrobKrongException(CommonErrorCode.BAD_REQUEST, "Each part number may appear only once");
        }
        s3.completeMultipartUpload(request -> request.bucket(props.getBucket())
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(upload -> upload.parts(completed)));
        return requireStored(key);
    }

    @Override
    public void abortMultipartUpload(String key, String uploadId) {
        s3.abortMultipartUpload(
                request -> request.bucket(props.getBucket()).key(key).uploadId(uploadId));
    }

    @Override
    public StoredObject upload(String key, InputStream content, String contentType) {
        int partSize = Math.toIntExact(props.getPartSize().toBytes());
        byte[] first = read(content, partSize);
        if (first.length < partSize) {
            byte[] md5 = ETags.md5(first);
            PutObjectResponse put = s3.putObject(
                    request -> request.bucket(props.getBucket())
                            .key(key)
                            .contentType(contentType)
                            .contentMD5(ETags.contentMd5(md5)),
                    RequestBody.fromBytes(first));
            requireETag(ETags.hex(md5), put.eTag(), key);
            return requireStored(key);
        }

        String uploadId = startMultipartUpload(key, contentType).uploadId();
        try {
            List<CompletedPart> completed = new ArrayList<>();
            List<byte[]> partMd5s = new ArrayList<>();
            for (byte[] chunk = first; chunk.length > 0; chunk = read(content, partSize)) {
                int partNumber = completed.size() + 1;
                if (partNumber > MAX_PARTS) {
                    throw new KrobKrongException(
                            CommonErrorCode.BAD_REQUEST, "Content needs more than " + MAX_PARTS + " parts");
                }
                byte[] body = chunk;
                byte[] md5 = ETags.md5(body);
                UploadPartResponse part = s3.uploadPart(
                        request -> request.bucket(props.getBucket())
                                .key(key)
                                .uploadId(uploadId)
                                .partNumber(partNumber)
                                .contentLength((long) body.length)
                                .contentMD5(ETags.contentMd5(md5)),
                        RequestBody.fromBytes(body));
                requireETag(ETags.hex(md5), part.eTag(), key + " part " + partNumber);
                completed.add(CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(part.eTag())
                        .build());
                partMd5s.add(md5);
            }
            CompleteMultipartUploadResponse done =
                    s3.completeMultipartUpload(request -> request.bucket(props.getBucket())
                            .key(key)
                            .uploadId(uploadId)
                            .multipartUpload(upload -> upload.parts(completed)));
            requireETag(ETags.multipart(partMd5s), done.eTag(), key);
            return requireStored(key);
        } catch (RuntimeException e) {
            abortQuietly(key, uploadId);
            throw e;
        }
    }

    @Override
    public Optional<StoredObject> stat(String key) {
        try {
            HeadObjectResponse head =
                    s3.headObject(request -> request.bucket(props.getBucket()).key(key));
            return Optional.of(new StoredObject(
                    key, ETags.unquote(head.eTag()), head.contentLength(), head.contentType(), head.lastModified()));
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(request -> request.bucket(props.getBucket()).key(key));
    }

    private StoredObject requireStored(String key) {
        return stat(key)
                .orElseThrow(() -> new KrobKrongException(
                        CommonErrorCode.STORAGE_ERROR, "Object " + key + " is missing after upload"));
    }

    private static void requireETag(String expected, String actual, String what) {
        if (!expected.equals(ETags.unquote(actual))) {
            throw new KrobKrongException(
                    CommonErrorCode.STORAGE_ERROR,
                    "ETag mismatch for " + what + ": expected " + expected + ", storage returned " + actual);
        }
    }

    private void abortQuietly(String key, String uploadId) {
        try {
            abortMultipartUpload(key, uploadId);
        } catch (SdkException e) {
            log.warn("Could not abort multipart upload {} for {}", uploadId, key, e);
        }
    }

    private static byte[] read(InputStream content, int length) {
        try {
            return content.readNBytes(length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Host is implied by the URL; clients (browsers especially) must not set it themselves.
    private static PresignedUrl toPresignedUrl(PresignedRequest presigned) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        presigned.signedHeaders().forEach((name, values) -> {
            if (!"host".equalsIgnoreCase(name)) {
                headers.put(name, values);
            }
        });
        return new PresignedUrl(
                presigned.url().toString(), presigned.httpRequest().method().name(), headers, presigned.expiration());
    }
}
