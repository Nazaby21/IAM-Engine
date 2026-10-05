package io.sala.krob_krong.common.exceptions;

import io.sala.krob_krong.common.errors.ErrorCode;

public class BusinessException extends KrobKrongException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
