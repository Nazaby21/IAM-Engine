package io.sala.krob_krong.common.utils.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.sala.krob_krong.common.errors.CommonErrorCode;
import io.sala.krob_krong.common.exceptions.KrobKrongException;
import io.sala.krob_krong.common.exceptions.S3ExceptionMapper;
import io.sala.krob_krong.common.utils.s3.dto.MultipartUpload;
import io.sala.krob_krong.common.utils.s3.dto.PartETag;
import io.sala.krob_krong.common.utils.s3.dto.PresignedPart;
import io.sala.krob_krong.common.utils.s3.dto.PresignedUrl;
import io.sala.krob_krong.common.utils.s3.dto.StoredObject;
import io.sala.krob_krong.common.utils.s3.dto.UploadedPart;
import io.sala.krob_krong.common.utils.s3.impl.S3UtilImpl;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;

// Runs against the RustFS from compose.yaml: docker compose up -d --wait && S3_IT=true ./gradlew test
@EnabledIfEnvironmentVariable(named = "S3_IT", matches = "true")
class S3UtilRustfsIT {

    private static final int PART_SIZE = (int) DataSize.ofMegabytes(5).toBytes();
    private static final String PREFIX = "it/" + UUID.randomUUID() + "/";

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static S3Client s3;
    private static S3UtilImpl s3Util;

    @BeforeAll
    static void connect() {
        S3Properties props = new S3Properties();
        props.setEndpoint(URI.create(System.getenv().getOrDefault("S3_IT_ENDPOINT", "http://localhost:9000")));
        props.setAccessKey(System.getenv().getOrDefault("S3_IT_ACCESS_KEY", "krobkrong"));
        props.setSecretKey(System.getenv().getOrDefault("S3_IT_SECRET_KEY", "krobkrong-secret"));
        props.setBucket("krob-krong-it");
        props.setPartSize(DataSize.ofMegabytes(5));
        S3Config config = new S3Config();
        s3 = config.s3Client(props);
        s3Util = new S3UtilImpl(s3, config.s3Presigner(props), props);
        new S3BucketInitializer(s3, props).createBucketIfMissing();
    }

    @AfterAll
    static void cleanUp() {
        s3.listObjectsV2Paginator(request -> request.bucket("krob-krong-it").prefix(PREFIX))
                .contents()
                .forEach(object -> s3Util.delete(object.key()));
    }

    @Test
    void presignedSingleUploadAndDownloadRoundTrip() throws Exception {
        String key = PREFIX + "hello.txt";
        byte[] content = "hello from krob-krong".getBytes();

        PresignedUrl upload = s3Util.presignUpload(key, "text/plain");
        HttpResponse<String> put = send(upload.url(), upload.headers(), content);
        assertThat(put.statusCode()).isEqualTo(200);

        StoredObject stored = s3Util.stat(key).orElseThrow();
        assertThat(stored.size()).isEqualTo(content.length);
        assertThat(stored.contentType()).isEqualTo("text/plain");
        assertThat(stored.eTag()).isEqualTo(ETags.hex(ETags.md5(content)));

        assertThat(download(s3Util.presignDownload(key))).isEqualTo(content);
    }

    @Test
    void presignedChunkedUploadCompletesWithClientETags() throws Exception {
        String key = PREFIX + "video.mp4";
        byte[] content = bytes(PART_SIZE * 2 + 4096);
        List<byte[]> chunks = chunk(content);

        MultipartUpload upload = s3Util.startMultipartUpload(key, "video/mp4");
        List<PresignedPart> urls = s3Util.presignUploadParts(key, upload.uploadId(), chunks.size());

        List<PartETag> eTags = new ArrayList<>();
        List<byte[]> md5s = new ArrayList<>();
        for (PresignedPart part : urls) {
            byte[] chunk = chunks.get(part.partNumber() - 1);
            HttpResponse<String> response = send(part.url(), part.headers(), chunk);
            assertThat(response.statusCode()).isEqualTo(200);
            String eTag = response.headers().firstValue("ETag").orElseThrow();
            assertThat(ETags.unquote(eTag)).isEqualTo(ETags.hex(ETags.md5(chunk)));
            eTags.add(new PartETag(part.partNumber(), eTag));
            md5s.add(ETags.md5(chunk));
        }

        assertThat(s3Util.listUploadedParts(key, upload.uploadId()))
                .extracting(UploadedPart::partNumber, UploadedPart::size)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, (long) PART_SIZE),
                        org.assertj.core.groups.Tuple.tuple(2, (long) PART_SIZE),
                        org.assertj.core.groups.Tuple.tuple(3, 4096L));

        StoredObject stored = s3Util.completeMultipartUpload(key, upload.uploadId(), eTags.reversed());
        assertThat(stored.size()).isEqualTo(content.length);
        assertThat(stored.eTag()).isEqualTo(ETags.multipart(md5s));
        assertThat(download(s3Util.presignDownload(key))).isEqualTo(content);
    }

    @Test
    void serverSideChunkedUploadVerifiesEveryETag() throws Exception {
        String key = PREFIX + "server-side.bin";
        byte[] content = bytes(PART_SIZE * 2 + 1);

        StoredObject stored = s3Util.upload(key, new ByteArrayInputStream(content), "application/octet-stream");

        assertThat(stored.size()).isEqualTo(content.length);
        assertThat(stored.eTag()).endsWith("-3");
        assertThat(download(s3Util.presignDownload(key))).isEqualTo(content);
    }

    @Test
    void smallServerSideUploadIsASinglePut() {
        byte[] content = bytes(2048);

        StoredObject stored = s3Util.upload(PREFIX + "small.bin", new ByteArrayInputStream(content), "image/png");

        assertThat(stored.eTag()).isEqualTo(ETags.hex(ETags.md5(content)));
    }

    @Test
    void completingWithAWrongETagIsAClientError() throws Exception {
        String key = PREFIX + "tampered.bin";
        MultipartUpload upload = s3Util.startMultipartUpload(key, "application/octet-stream");
        PresignedPart part =
                s3Util.presignUploadParts(key, upload.uploadId(), 1).getFirst();
        send(part.url(), part.headers(), bytes(1024));

        assertThatThrownBy(() -> s3Util.completeMultipartUpload(
                        key, upload.uploadId(), List.of(new PartETag(1, "00000000000000000000000000000000"))))
                .isInstanceOfSatisfying(
                        SdkException.class,
                        e -> assertThat(mapped(e).httpStatus()).isEqualTo(CommonErrorCode.BAD_REQUEST.httpStatus()));
        s3Util.abortMultipartUpload(key, upload.uploadId());
    }

    @Test
    void abortedUploadNoLongerExists() throws Exception {
        String key = PREFIX + "aborted.bin";
        MultipartUpload upload = s3Util.startMultipartUpload(key, "application/octet-stream");
        PresignedPart part =
                s3Util.presignUploadParts(key, upload.uploadId(), 1).getFirst();
        send(part.url(), part.headers(), bytes(1024));

        s3Util.abortMultipartUpload(key, upload.uploadId());

        assertThatThrownBy(() -> s3Util.listUploadedParts(key, upload.uploadId()))
                .isInstanceOfSatisfying(
                        SdkException.class, e -> assertThat(mapped(e).httpStatus())
                                .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND.httpStatus()));
        assertThat(s3Util.stat(key)).isEmpty();
    }

    private static KrobKrongException mapped(SdkException e) {
        return new S3ExceptionMapper().map(e);
    }

    private static HttpResponse<String> send(String url, Map<String, List<String>> headers, byte[] body)
            throws Exception {
        HttpRequest.Builder request =
                HttpRequest.newBuilder(URI.create(url)).PUT(HttpRequest.BodyPublishers.ofByteArray(body));
        headers.forEach((name, values) -> values.forEach(value -> request.header(name, value)));
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static byte[] download(PresignedUrl url) throws Exception {
        HttpResponse<byte[]> response = HTTP.send(
                HttpRequest.newBuilder(URI.create(url.url())).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    private static List<byte[]> chunk(byte[] content) {
        List<byte[]> chunks = new ArrayList<>();
        for (int offset = 0; offset < content.length; offset += PART_SIZE) {
            chunks.add(Arrays.copyOfRange(content, offset, Math.min(content.length, offset + PART_SIZE)));
        }
        return chunks;
    }

    private static byte[] bytes(int size) {
        byte[] content = new byte[size];
        new Random(size).nextBytes(content);
        return content;
    }
}
