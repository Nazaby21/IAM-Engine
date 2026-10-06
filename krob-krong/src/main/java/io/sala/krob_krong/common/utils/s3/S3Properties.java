package io.sala.krob_krong.common.utils.s3;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "krob-krong.storage.s3")
public class S3Properties {

    static final DataSize MIN_PART_SIZE = DataSize.ofMegabytes(5);

    @NotNull
    private URI endpoint;

    // Address clients use for presigned URLs when it differs from the server's (e.g. Docker network vs browser).
    private URI publicEndpoint;

    @NotBlank
    private String region = "us-east-1";

    @NotBlank
    private String accessKey;

    @NotBlank
    private String secretKey;

    @NotBlank
    private String bucket;

    private boolean pathStyleAccess = true;

    private boolean autoCreateBucket = false;

    @NotNull
    private Duration presignTtl = Duration.ofMinutes(15);

    @NotNull
    private DataSize partSize = DataSize.ofMegabytes(8);

    public URI presignEndpoint() {
        return publicEndpoint != null ? publicEndpoint : endpoint;
    }

    @AssertTrue(message = "part-size must be at least 5MB, the S3 minimum for every part but the last")
    public boolean isPartSizeValid() {
        return partSize == null || partSize.compareTo(MIN_PART_SIZE) >= 0;
    }
}
