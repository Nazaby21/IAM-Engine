package io.sala.krob_krong.iam.security.web;

import io.sala.krob_krong.common.errors.CommonErrorCode;
import io.sala.krob_krong.common.exceptions.ExceptionMapper;
import io.sala.krob_krong.common.exceptions.KrobKrongException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {

    @Override
    public Class<AuthenticationException> supportedType() {
        return AuthenticationException.class;
    }

    @Override
    public KrobKrongException map(AuthenticationException exception) {
        String message = exception instanceof OAuth2AuthenticationException
                ? "The access token is invalid, expired or revoked"
                : "Authentication is required";
        return new KrobKrongException(CommonErrorCode.UNAUTHORIZED, message);
    }
}
