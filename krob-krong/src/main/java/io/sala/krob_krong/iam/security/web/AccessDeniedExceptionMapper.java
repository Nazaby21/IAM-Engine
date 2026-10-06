package io.sala.krob_krong.iam.security.web;

import io.sala.krob_krong.common.errors.CommonErrorCode;
import io.sala.krob_krong.common.exceptions.ExceptionMapper;
import io.sala.krob_krong.common.exceptions.KrobKrongException;
import io.sala.krob_krong.iam.security.Principals;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

// @PreAuthorize denials are thrown inside the DispatcherServlet, so they reach the controller advice.
@Component
public class AccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {

    @Override
    public Class<AccessDeniedException> supportedType() {
        return AccessDeniedException.class;
    }

    @Override
    public KrobKrongException map(AccessDeniedException exception) {
        if (Principals.current().isEmpty()) {
            return new KrobKrongException(CommonErrorCode.UNAUTHORIZED, "Authentication is required");
        }
        return new KrobKrongException(CommonErrorCode.FORBIDDEN, "You do not have permission to perform this action");
    }
}
