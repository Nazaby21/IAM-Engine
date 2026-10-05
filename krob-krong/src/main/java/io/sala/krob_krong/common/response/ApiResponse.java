package io.sala.krob_krong.common.response;

import io.sala.krob_krong.common.errors.ApiError;
import io.sala.krob_krong.common.utils.TraceContext;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

@Getter
@Setter
@AllArgsConstructor
public class ApiResponse<T> {

    private T data;
    private ApiError error;
    private String requestId;
    private String traceId;
    private Instant timestamp;

    // -- Factory Methods --
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null, requestId(), traceId(), Instant.now());
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
