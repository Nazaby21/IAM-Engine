package io.sala.krob_krong.common.exceptions;

import io.sala.krob_krong.common.errors.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
public class S3ExceptionMapper implements ExceptionMapper<SdkException> {

    @Override
    public Class<SdkException> supportedType() {
        return SdkException.class;
    }

    @Override
    public KrobKrongException map(SdkException exception) {
        if (exception instanceof S3Exception s3 && s3.awsErrorDetails() != null) {
            String code = s3.awsErrorDetails().errorCode();
            String message = code + ": " + s3.awsErrorDetails().errorMessage();
            if (s3.statusCode() == 404 && !"NoSuchBucket".equals(code)) {
                return new KrobKrongException(CommonErrorCode.RESOURCE_NOT_FOUND, message);
            }
            if (s3.statusCode() == 400) {
                return new KrobKrongException(CommonErrorCode.BAD_REQUEST, message);
            }
        }
        log.error("Object storage request failed", exception);
        return new KrobKrongException(CommonErrorCode.STORAGE_ERROR);
    }
}
