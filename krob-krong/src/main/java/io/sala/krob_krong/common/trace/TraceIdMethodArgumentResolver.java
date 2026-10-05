package io.sala.krob_krong.common.trace;

import io.sala.krob_krong.common.annotations.RequestId;
import io.sala.krob_krong.common.annotations.TraceId;
import io.sala.krob_krong.common.utils.TraceContext;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class TraceIdMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasMethodAnnotation(RequestId.class) || parameter.hasMethodAnnotation(TraceId.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        if (parameter.hasParameterAnnotation(RequestId.class)) {
            String requestId = MDC.get(TraceContext.MDC_REQUEST_ID);
            return (StringUtils.isNotBlank(requestId)) ? requestId : "unknown";
        }
        if (parameter.hasParameterAnnotation(TraceId.class)) {
            return MDC.get(TraceContext.MDC_TRACE_ID); // may be null
        }
        return null;
    }
}
