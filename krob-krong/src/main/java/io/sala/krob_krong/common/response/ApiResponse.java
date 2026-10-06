package io.sala.krob_krong.common.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.sala.krob_krong.common.errors.ApiError;
import io.sala.krob_krong.common.utils.TraceContext;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ApiResponse<T> {

    private T data;
    private ApiError error;
    private String requestId;
    private String traceId;
    private Instant timestamp;

    // Transport metadata only; the JSON envelope stays unchanged.
    @JsonIgnore
    @Setter(lombok.AccessLevel.NONE)
    private HttpStatus httpStatus;

    public ApiResponse(T data, ApiError error, String requestId, String traceId, Instant timestamp) {
        this.data = data;
        this.error = error;
        this.requestId = requestId;
        this.traceId = traceId;
        this.timestamp = timestamp;
    }

    // -- Factory Methods --
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null, requestId(), traceId(), Instant.now());
    }

    public static <T> ApiResponse<T> create(T data) {
        ApiResponse<T> response = success(data);
        response.httpStatus = HttpStatus.CREATED;
        return response;
    }

    public static ApiResponse<Void> error(ApiError error) {
        return new ApiResponse<>(null, error, requestId(), traceId(), Instant.now());
    }

    public static <T> ApiResponse<PageResponse<T>> paginated(PageResponse<T> page) {
        return new ApiResponse<>(page, null, requestId(), traceId(), Instant.now());
    }

    // -- MDC helpers --
    private static String requestId() {
        String v = MDC.get(TraceContext.MDC_REQUEST_ID);
        return StringUtils.isNotBlank(v) ? v : UUID.randomUUID().toString();
    }

    private static String traceId() {
        String v = MDC.get("traceId");
        if (StringUtils.isNotBlank(v)) return v;
        v = MDC.get(TraceContext.MDC_TRACE_ID);
        return StringUtils.isNotBlank(v) ? v : null;
    }
}
