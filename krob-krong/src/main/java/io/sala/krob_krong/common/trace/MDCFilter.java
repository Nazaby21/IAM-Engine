package io.sala.krob_krong.common.trace;

import io.sala.krob_krong.common.utils.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class MDCFilter extends OncePerRequestFilter {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final String ATTRIBUTE_PREFIX = MDCFilter.class.getName() + ".";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {

        String requestId = resolveId(request, TraceContext.REQUEST_ID_HEADER);
        String traceId = resolveId(request, TraceContext.TRACE_ID_HEADER);

        MDC.put(TraceContext.MDC_REQUEST_ID, requestId);
        MDC.put(TraceContext.MDC_TRACE_ID, traceId);
        response.setHeader(TraceContext.REQUEST_ID_HEADER, requestId);
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TraceContext.MDC_REQUEST_ID);
            MDC.remove(TraceContext.MDC_TRACE_ID);
        }
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private static String resolveId(HttpServletRequest request, String header) {
        String attribute = ATTRIBUTE_PREFIX + header;
        if (request.getAttribute(attribute) instanceof String existing) return existing;

        String value = request.getHeader(header);
        String id = (value != null && SAFE_ID.matcher(value).matches())
                ? value
                : UUID.randomUUID().toString();
        request.setAttribute(attribute, id);
        return id;
    }
}
