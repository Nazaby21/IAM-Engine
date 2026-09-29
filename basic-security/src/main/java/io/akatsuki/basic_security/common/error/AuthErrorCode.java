package io.akatsuki.basic_security.common.error;

import io.acmwchsd.core.error.ErrorCategory;
import io.acmwchsd.core.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    INVALID_CREDENTIAL(401, ErrorCategory.AUTHENTICATION, "Invalid Credential");

    private final int httpStatus;
    private final ErrorCategory category;
    private final String defaultMessage;

    AuthErrorCode(int httpStatus, ErrorCategory category, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.category = category;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    @Override
    public ErrorCategory category() {
        return category;
    }
}
