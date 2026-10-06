package io.sala.krob_krong.common.utils.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import io.sala.krob_krong.common.exceptions.KrobKrongException;
import io.sala.krob_krong.common.utils.s3.dto.PresignedUrl;
import io.sala.krob_krong.common.utils.s3.dto.StoredObject;
import io.sala.krob_krong.common.utils.s3.impl.S3UtilImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

class S3UtilImplTest {

    private static final int PART_SIZE = (int) DataSize.ofMegabytes(5).toBytes();

    private final S3Client s3 = mock(S3Client.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    private final List<UploadPartRequest> uploadedParts = new ArrayList<>();
    private final List<byte[]> partMd5s = new ArrayList<>();
    private S3Properties props;
    private S3UtilImpl s3Util;

    @BeforeEach
    void setUp() {
        props = new S3Properties();
        props.setEndpoint(URI.create("http://localhost:9000"));
        props.setAccessKey("krobkrong");
        props.setSecretKey("krobkrong-secret");
        props.setBucket("bucket");
        props.setPartSize(DataSize.ofMegabytes(5));
        s3Util = new S3UtilImpl(s3, new S3Config().s3Presigner(props), props);

        doReturn(CreateMultipartUploadResponse.builder().uploadId("upload-1").build())
                .when(s3)
                .createMultipartUpload(any(CreateMultipartUploadRequest.class));
        doReturn(AbortMultipartUploadResponse.builder().build())
                .when(s3)
                .abortMultipartUpload(any(AbortMultipartUploadRequest.class));
        doAnswer(invocation -> HeadObjectResponse.builder()
                        .eTag("\"stored\"")
                        .contentLength(42L)
                        .build())
                .when(s3)
                .headObject(any(HeadObjectRequest.class));
    }

    @Test
    void contentSmallerThanOnePartIsStoredWithASinglePut() {
        byte[] content = bytes(1024);
        doAnswer(invocation -> PutObjectResponse.builder()
                        .eTag(ETags.quote(ETags.hex(ETags.md5(content))))
                        .build())
                .when(s3)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        s3Util.upload("small.bin", new ByteArrayInputStream(content), "application/octet-stream");

        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(put.capture(), any(RequestBody.class));
        assertThat(put.getValue().contentMD5()).isEqualTo(ETags.contentMd5(ETags.md5(content)));
        verify(s3, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
    }

    @Test
    void largeContentIsUploadedInPartsAndCompletedWithTheirETags() {
        byte[] content = bytes(PART_SIZE * 2 + 1234);
        givenStorageEchoesPartMd5s();
        givenCompletionReturns(null);

        StoredObject stored = s3Util.upload("large.bin", new ByteArrayInputStream(content), "video/mp4");

        assertThat(uploadedParts).extracting(UploadPartRequest::partNumber).containsExactly(1, 2, 3);
        assertThat(uploadedParts)
                .extracting(UploadPartRequest::contentLength)
                .containsExactly((long) PART_SIZE, (long) PART_SIZE, 1234L);
        assertThat(uploadedParts)
                .allSatisfy(part -> assertThat(part.contentMD5()).isNotBlank());

        ArgumentCaptor<CompleteMultipartUploadRequest> complete =
                ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        verify(s3).completeMultipartUpload(complete.capture());
        assertThat(complete.getValue().multipartUpload().parts())
                .extracting(part -> ETags.unquote(part.eTag()))
                .containsExactlyElementsOf(partMd5s.stream().map(ETags::hex).toList());
        assertThat(stored.eTag()).isEqualTo("stored");
        verify(s3, never()).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
    }

    @Test
    void partWhoseETagDoesNotMatchItsMd5AbortsTheUpload() {
        doReturn(UploadPartResponse.builder().eTag("\"not-the-md5\"").build())
                .when(s3)
                .uploadPart(any(UploadPartRequest.class), any(RequestBody.class));

        assertThatThrownBy(() -> s3Util.upload("x.bin", new ByteArrayInputStream(bytes(PART_SIZE + 1)), "video/mp4"))
                .isInstanceOf(KrobKrongException.class)
                .hasMessageContaining("ETag mismatch for x.bin part 1");
        verify(s3).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
    }

    @Test
    void assembledObjectWithUnexpectedETagIsRejected() {
        givenStorageEchoesPartMd5s();
        givenCompletionReturns("\"0123456789abcdef0123456789abcdef-2\"");

        assertThatThrownBy(() -> s3Util.upload("x.bin", new ByteArrayInputStream(bytes(PART_SIZE + 1)), "video/mp4"))
                .isInstanceOf(KrobKrongException.class)
                .hasMessageContaining("ETag mismatch for x.bin:");
    }

    @Test
    void storageOrStreamFailureMidUploadAbortsTheUpload() {
        givenStorageEchoesPartMd5s();
        var failingStream = new ByteArrayInputStream(bytes(PART_SIZE)) {
            private boolean firstRead = true;

            @Override
            public byte[] readNBytes(int len) throws IOException {
                if (firstRead) {
                    firstRead = false;
                    return super.readNBytes(len);
                }
                throw new IOException("client disconnected");
            }
        };

        assertThatThrownBy(() -> s3Util.upload("x.bin", failingStream, "video/mp4"))
                .isInstanceOf(UncheckedIOException.class);
        verify(s3).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
    }

    @Test
    void missingObjectStatIsEmptyRatherThanAnError() {
        doAnswer(invocation -> {
                    throw S3Exception.builder().statusCode(404).build();
                })
                .when(s3)
                .headObject(any(HeadObjectRequest.class));

        assertThat(s3Util.stat("missing.bin")).isEmpty();
    }

    @Test
    void presignedUploadIsPathStyleAndCarriesNoChecksumParameters() {
        PresignedUrl upload = s3Util.presignUpload("schools/1/logo.png", "image/png");

        assertThat(upload.method()).isEqualTo("PUT");
        assertThat(upload.url()).startsWith("http://localhost:9000/bucket/schools/1/logo.png?");
        assertThat(upload.url()).doesNotContainIgnoringCase("checksum");
        assertThat(upload.headers()).containsKey("content-type").doesNotContainKey("host");
    }

    @Test
    void presignedPartsCarryTheirPartNumberAndUploadId() {
        var parts = s3Util.presignUploadParts("big.mp4", "upload-1", 3);

        assertThat(parts).extracting(part -> part.partNumber()).containsExactly(1, 2, 3);
        assertThat(parts.get(1).url()).contains("partNumber=2").contains("uploadId=upload-1");
        assertThatThrownBy(() -> s3Util.presignUploadParts("big.mp4", "upload-1", 10_001))
                .isInstanceOf(KrobKrongException.class);
    }

    @Test
    void multipartETagFollowsTheS3Convention() {
        byte[] first = ETags.md5("a".getBytes());
        byte[] second = ETags.md5("b".getBytes());
        byte[] concatenated = new byte[32];
        System.arraycopy(first, 0, concatenated, 0, 16);
        System.arraycopy(second, 0, concatenated, 16, 16);

        assertThat(ETags.multipart(List.of(first, second))).isEqualTo(ETags.hex(ETags.md5(concatenated)) + "-2");
        assertThat(ETags.unquote("\"abc\"")).isEqualTo("abc");
        assertThat(ETags.quote("abc")).isEqualTo("\"abc\"");
    }

    private void givenStorageEchoesPartMd5s() {
        doAnswer(invocation -> {
                    UploadPartRequest request = invocation.getArgument(0);
                    RequestBody body = invocation.getArgument(1);
                    byte[] md5 =
                            ETags.md5(body.contentStreamProvider().newStream().readAllBytes());
                    uploadedParts.add(request);
                    partMd5s.add(md5);
                    return UploadPartResponse.builder()
                            .eTag(ETags.quote(ETags.hex(md5)))
                            .build();
                })
                .when(s3)
                .uploadPart(any(UploadPartRequest.class), any(RequestBody.class));
    }

    private void givenCompletionReturns(String eTagOverride) {
        doAnswer(invocation -> CompleteMultipartUploadResponse.builder()
                        .eTag(eTagOverride != null ? eTagOverride : ETags.quote(ETags.multipart(partMd5s)))
                        .build())
                .when(s3)
                .completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
    }

    private static byte[] bytes(int size) {
        byte[] content = new byte[size];
        new Random(size).nextBytes(content);
        return content;
    }
}
