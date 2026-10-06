package io.sala.krob_krong.common.errors;

public enum CommonErrorCode implements ErrorCode {
    INTERNAL_ERROR(500, ErrorCategory.INTERNAL, "Internal server error"),
    BAD_REQUEST(400, ErrorCategory.VALIDATION, "Bad request"),
    UNAUTHORIZED(401, ErrorCategory.AUTHENTICATION, "Unauthorized"),
    FORBIDDEN(403, ErrorCategory.AUTHORIZATION, "Access denied"),
    RESOURCE_NOT_FOUND(404, ErrorCategory.NOT_FOUND, "Resource not found"),
    METHOD_NOT_ALLOWED(405, ErrorCategory.VALIDATION, "Method not allowed"),
    CONFLICT(409, ErrorCategory.VALIDATION, "Conflict"),
    STORAGE_ERROR(502, ErrorCategory.INTERNAL, "Object storage request failed"),
    UNSUPPORTED_MEDIA_TYPE(415, ErrorCategory.VALIDATION, "Unsupported media type");

    private final int httpStatus;
    private final ErrorCategory category;
    private final String defaultMessage;

    CommonErrorCode(int httpStatus, ErrorCategory category, String defaultMessage) {
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
    public ErrorCategory category() {
        return category;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
