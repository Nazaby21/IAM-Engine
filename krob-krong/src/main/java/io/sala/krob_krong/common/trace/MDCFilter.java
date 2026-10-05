package io.sala.krob_krong.common.trace;

import io.sala.krob_krong.common.utils.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class MDCFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {

        String requestId = resolveRequestId(request);

        try {
            MDC.put(TraceContext.MDC_REQUEST_ID, requestId);

            String earlyTraceId = resolveTraceId(request);
            MDC.put(TraceContext.MDC_TRACE_ID, earlyTraceId);

            response.setHeader(TraceContext.REQUEST_ID_HEADER, requestId);

            chain.doFilter(request, response);

            String traceId = resolveTraceId(request);
            MDC.put(TraceContext.MDC_TRACE_ID, traceId);
            response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);

            String spanId = resolveSpanId();
            if (ObjectUtils.isNotEmpty(spanId)) {
                MDC.put(TraceContext.MDC_SPAN_ID, spanId);
                response.setHeader(TraceContext.SPAN_ID_HEADER, spanId);
            }
        } finally {
            MDC.remove(TraceContext.MDC_REQUEST_ID);
            MDC.remove(TraceContext.MDC_TRACE_ID);
            MDC.remove(TraceContext.MDC_SPAN_ID);
        }
    }

    private static String resolveRequestId(HttpServletRequest request) {
        String header = request.getHeader(TraceContext.REQUEST_ID_HEADER);
        return (StringUtils.isNotBlank(header)) ? header : UUID.randomUUID().toString();
    }

    private static String resolveTraceId(HttpServletRequest request) {
        String traceId = MDC.get(TraceContext.MDC_TRACE_ID);
        if (StringUtils.isNotBlank(traceId)) return traceId;

        traceId = MDC.get("traceId");
        if (StringUtils.isNotBlank(traceId)) return traceId;

        String header = request.getHeader(TraceContext.TRACE_ID_HEADER);
        if (StringUtils.isNotBlank(header)) return header;

        return UUID.randomUUID().toString();
    }

    private static String resolveSpanId() {
        String spanId = MDC.get(TraceContext.MDC_SPAN_ID);
        if (StringUtils.isNotBlank(spanId)) return spanId;

        spanId = MDC.get("spanId");
        return StringUtils.isNotBlank(spanId) ? spanId : null;
    }
}
