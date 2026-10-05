package io.sala.krob_krong.common.exceptions;

import io.sala.krob_krong.common.errors.ErrorCategory;
import io.sala.krob_krong.common.errors.ErrorCode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class KrobKrongException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;
    private final ErrorCategory category;
    private final Map<String, Object> context = new LinkedHashMap<>();

    // ---------------------------------------------------------------------
    // Raw constructors
    // ---------------------------------------------------------------------
    public KrobKrongException(String errorCode, String message, int httpStatus) {
        this(errorCode, message, httpStatus, ErrorCategory.INTERNAL, null);
    }

    public KrobKrongException(String errorCode, String message, int httpStatus, Throwable cause) {
        this(errorCode, message, httpStatus, ErrorCategory.INTERNAL, cause);
    }

    public KrobKrongException(String errorCode, String message, int httpStatus, ErrorCategory category) {
        this(errorCode, message, httpStatus, category, null);
    }

    public KrobKrongException(
            String errorCode, String message, int httpStatus, ErrorCategory category, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.category = (category != null) ? category : ErrorCategory.INTERNAL;
    }

    public KrobKrongException(String errorCode, String message) {
        this(errorCode, message, 500, ErrorCategory.INTERNAL, null);
    }

    // ---------------------------------------------------------------------
    // ErrorCode constructors
    // ---------------------------------------------------------------------
    public KrobKrongException(ErrorCode errorCode) {
        this(errorCode.code(), errorCode.defaultMessage(), errorCode.httpStatus(), errorCode.category(), null);
    }

    public KrobKrongException(ErrorCode errorCode, String message) {
        this(errorCode.code(), message, errorCode.httpStatus(), errorCode.category(), null);
    }

    public KrobKrongException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode.code(), message, errorCode.httpStatus(), errorCode.category(), cause);
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------
    public String errorCode() {
        return errorCode;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public ErrorCategory category() {
        return category;
    }

    public Map<String, Object> context() {
        return Collections.unmodifiableMap(context);
    }

    public KrobKrongException withContext(String key, Object value) {
        if (key != null) context.put(key, value);
        return this;
    }

    public KrobKrongException withContext(Map<String, ?> entries) {
        if (entries != null) context.putAll(entries);
        return this;
    }
}
