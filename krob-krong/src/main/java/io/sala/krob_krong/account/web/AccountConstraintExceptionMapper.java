package io.sala.krob_krong.account.web;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.exceptions.ExceptionMapper;
import io.sala.krob_krong.common.exceptions.KrobKrongException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import org.postgresql.util.PSQLException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

/** Keeps concurrent-registration conflicts consistent with the preflight email check. */
@Component
public class AccountConstraintExceptionMapper implements ExceptionMapper<NestedRuntimeException> {
    @Override
    public Class<NestedRuntimeException> supportedType() {
        return NestedRuntimeException.class;
    }

    @Override
    public KrobKrongException map(NestedRuntimeException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            if (cause instanceof PSQLException sql
                    && "23505".equals(sql.getSQLState())
                    && sql.getServerErrorMessage() != null
                    && "app_user_email_uq".equals(sql.getServerErrorMessage().getConstraint())) {
                return new BusinessException(IAMErrorCode.EMAIL_ALREADY_REGISTERED);
            }
        }
        return null;
    }
}
