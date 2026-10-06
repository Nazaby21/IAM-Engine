package io.sala.krob_krong.iam.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver exceptionResolver;

    public RestAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticate(ex));
        exceptionResolver.resolveException(request, response, null, ex);
    }

    // RFC 6750 §3: error code only; the description could echo token parsing details.
    private static String wwwAuthenticate(AuthenticationException ex) {
        if (ex instanceof OAuth2AuthenticationException oauth2) {
            return "Bearer error=\"" + oauth2.getError().getErrorCode() + "\"";
        }
        return "Bearer";
    }
}
