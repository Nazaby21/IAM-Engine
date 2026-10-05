package io.sala.krob_krong.common.errors;

public interface ErrorCode {
    String code();

    int httpStatus();

    String defaultMessage();

    default ErrorCategory category() {
        return ErrorCategory.INTERNAL;
    }
}
