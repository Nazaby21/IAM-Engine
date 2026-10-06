package io.sala.krob_krong.common.utils.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "krob-krong.storage.s3", name = "auto-create-bucket", havingValue = "true")
public class S3BucketInitializer {

    private final S3Client s3;
    private final S3Properties props;

    @EventListener(ApplicationReadyEvent.class)
    public void createBucketIfMissing() {
        String bucket = props.getBucket();
        try {
            if (!exists(bucket)) {
                s3.createBucket(request -> request.bucket(bucket));
                log.info("Created object storage bucket {}", bucket);
            }
        } catch (SdkException e) {
            log.warn("Object storage at {} is unreachable; bucket {} was not checked", props.getEndpoint(), bucket, e);
        }
    }

    private boolean exists(String bucket) {
        try {
            s3.headBucket(request -> request.bucket(bucket));
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
