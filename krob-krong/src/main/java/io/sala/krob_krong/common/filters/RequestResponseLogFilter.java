package io.sala.krob_krong.common.filters;

import io.sala.krob_krong.common.log.MaskedLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Order(5)
public class RequestResponseLogFilter extends OncePerRequestFilter {

    private static final Set<String> BINARY_PREFIXES =
            Set.of("image/", "audio/", "video/", "application/octet-stream", "multipart/");

    private final MaskedLogger log;
    private final boolean enabled;
    private final int maxBodySize;
    private final List<String> excludedPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RequestResponseLogFilter(MaskedLogger logger, boolean enabled, int maxBodySize, List<String> excludedPaths) {
        this.log = logger;
        this.enabled = enabled;
        this.maxBodySize = maxBodySize;
        this.excludedPaths = excludedPaths != null ? excludedPaths : List.of();
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!enabled) return true;
        String path = request.getRequestURI();
        for (String pattern : excludedPaths) {
            if (pathMatcher.match(pattern, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {

        boolean cacheRequestBody = isLoggableContentType(request.getContentType());

        ContentCachingRequestWrapper wrappedRequest =
                cacheRequestBody ? new ContentCachingRequestWrapper(request, maxBodySize) : null;
        BoundedBodyCaptureResponseWrapper wrappedResponse =
                new BoundedBodyCaptureResponseWrapper(response, maxBodySize);

        long start = System.nanoTime();
        try {
            chain.doFilter(cacheRequestBody ? wrappedRequest : request, wrappedResponse);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            logRequest(cacheRequestBody ? wrappedRequest : request, cacheRequestBody);
            logResponse(request, wrappedResponse, durationMs);
        }
    }

    private void logRequest(HttpServletRequest request, boolean includeBody) {
        Map<String, String> headers = extractHeaders(request);
        Map<String, String> maskedHeaders = log.maskHeaders(headers);

        String body = "";
        if (includeBody && request instanceof ContentCachingRequestWrapper cached) {
            byte[] buf = cached.getContentAsByteArray();
            if (buf.length > 0) {
                body = log.maskBody(new String(buf, StandardCharsets.UTF_8), request.getContentType());
            }
        }

        log.info(
                "[INBOUND] >> {} {} | headers: {} | body: {}",
                request.getMethod(),
                request.getRequestURI(),
                maskedHeaders,
                body);
    }

    private void logResponse(HttpServletRequest request, BoundedBodyCaptureResponseWrapper response, long durationMs) {
        String body = "";
        if (isLoggableContentType(response.getContentType())) {
            String captured = response.capturedBody();
            if (!captured.isEmpty()) {
                body = log.maskBody(captured, response.getContentType());
            }
        }

        log.info(
                "[OUTBOUND] << {} {} | status: {} | duration: {}ms | body: {}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMs,
                body);
    }

    private static Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    /** Text-ish payloads are loggable; binary payloads (and no content type) are not. */
    private static boolean isLoggableContentType(String contentType) {
        if (contentType == null) return true;
        String ct = contentType.toLowerCase();
        for (String prefix : BINARY_PREFIXES) {
            if (ct.startsWith(prefix)) return false;
        }
        return true;
    }
}
