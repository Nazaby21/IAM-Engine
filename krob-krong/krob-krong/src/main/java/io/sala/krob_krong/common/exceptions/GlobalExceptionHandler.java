package io.sala.krob_krong.common.exceptions;

import io.sala.krob_krong.common.errors.ApiError;
import io.sala.krob_krong.common.errors.CommonErrorCode;
import io.sala.krob_krong.common.errors.ErrorCategory;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.common.utils.TraceContext;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ExceptionTranslator translator;
    private final boolean exposeMessageOnUnknown;

    public GlobalExceptionHandler(List<ExceptionMapper<?>> mappers, boolean exposeMessageOnUnknown) {
        this.translator = new ExceptionTranslator(mappers, exposeMessageOnUnknown);
        this.exposeMessageOnUnknown = exposeMessageOnUnknown;
    }

    // -----------------------------------------------------------------------
    // KrobKrongException family (covers every framework's exceptions)
    // -----------------------------------------------------------------------
    @ExceptionHandler(KrobKrongException.class)
    public ResponseEntity<ApiResponse<Void>> handleKrobKrongException(KrobKrongException ex) {
        return build(ex);
    }

    // -----------------------------------------------------------------------
    // Spring MVC & Bean Validation
    // -----------------------------------------------------------------------
    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        ApiError error = ApiError.of(
                CommonErrorCode.BAD_REQUEST.code(),
                ex.getMessage() != null ? ex.getMessage() : CommonErrorCode.BAD_REQUEST.defaultMessage(),
                ErrorCategory.VALIDATION);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        ApiError error =
                ApiError.of(CommonErrorCode.METHOD_NOT_ALLOWED.code(), ex.getMessage(), ErrorCategory.VALIDATION);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.error(error));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMedia(HttpMediaTypeNotSupportedException ex) {
        ApiError error =
                ApiError.of(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.code(), ex.getMessage(), ErrorCategory.VALIDATION);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ApiResponse.error(error));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandler(NoHandlerFoundException ex) {
        ApiError error = ApiError.of(
                CommonErrorCode.RESOURCE_NOT_FOUND.code(),
                "No handler for " + ex.getHttpMethod() + " " + ex.getRequestURL(),
                ErrorCategory.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    // -----------------------------------------------------------------------
    // Catch-all — runs registered ExceptionMappers, then INTERNAL_ERROR
    // -----------------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        KrobKrongException mapped = translator.toKrobKrongException(ex);
        if (mapped != null) return build(mapped);

        String requestId = MDC.get(TraceContext.MDC_REQUEST_ID);
        log.error("Unhandled exception [requestId={}]", requestId != null ? requestId : "unknown", ex);
        ApiError error = ApiError.of(
                CommonErrorCode.INTERNAL_ERROR.code(),
                exposeMessageOnUnknown && ex.getMessage() != null
                        ? ex.getMessage()
                        : CommonErrorCode.INTERNAL_ERROR.defaultMessage(),
                ErrorCategory.INTERNAL);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private static ResponseEntity<ApiResponse<Void>> build(KrobKrongException ex) {
        return ResponseEntity.status(ex.httpStatus()).body(ApiResponse.error(ExceptionTranslator.buildApiError(ex)));
    }
}
