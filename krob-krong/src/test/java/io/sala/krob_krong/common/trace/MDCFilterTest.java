package io.sala.krob_krong.common.trace;

import static org.assertj.core.api.Assertions.assertThat;

import io.sala.krob_krong.common.utils.TraceContext;
import jakarta.servlet.DispatcherType;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MDCFilterTest {

    private final MDCFilter filter = new MDCFilter();

    @Test
    void generatesTraceIdWhenNoneIsSent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> seen.set(MDC.get(TraceContext.MDC_TRACE_ID)));

        assertThat(seen.get()).isNotBlank();
        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER)).isEqualTo(seen.get());
        assertThat(MDC.get(TraceContext.MDC_TRACE_ID)).isNull();
    }

    @Test
    void propagatesIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContext.TRACE_ID_HEADER, "4bf92f3577b34da6a3ce929d0e0e4736");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER)).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void replacesUnsafeIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContext.TRACE_ID_HEADER, "abc\r\nforged-log-line");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER))
                .isNotBlank()
                .doesNotContain("forged");
    }

    @Test
    void errorDispatchKeepsTheOriginalTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        String original = response.getHeader(TraceContext.TRACE_ID_HEADER);

        request.setDispatcherType(DispatcherType.ERROR);
        MockHttpServletResponse errorResponse = new MockHttpServletResponse();
        filter.doFilter(request, errorResponse, new MockFilterChain());

        assertThat(errorResponse.getHeader(TraceContext.TRACE_ID_HEADER)).isEqualTo(original);
    }
}
