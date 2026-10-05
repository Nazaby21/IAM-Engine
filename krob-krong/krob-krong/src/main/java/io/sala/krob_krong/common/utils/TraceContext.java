package io.sala.krob_krong.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TraceContext {
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String TRACE_ID_HEADER = "X-Trace-ID";
    public static final String SPAN_ID_HEADER = "X-Span-ID";

    public static final String MDC_REQUEST_ID = "request_id";
    public static final String MDC_TRACE_ID = "trace_id";
    public static final String MDC_SPAN_ID = "span_id";
}
