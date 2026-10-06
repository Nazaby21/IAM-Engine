package io.sala.krob_krong.common.exceptions;

import io.sala.krob_krong.common.errors.CommonErrorCode;
import java.sql.SQLException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

/** Maps errors raised by the database write guards (and constraint violations) to client errors instead of 500s. */
@Component
public class SqlStateExceptionMapper implements ExceptionMapper<NestedRuntimeException> {

    static final String INSUFFICIENT_PRIVILEGE = "42501";
    static final String CHECK_VIOLATION = "23514";
    static final String RAISE_EXCEPTION = "P0001";
    static final String UNIQUE_VIOLATION = "23505";
    static final String EXCLUSION_VIOLATION = "23P01";
    static final String FOREIGN_KEY_VIOLATION = "23503";

    @Override
    public Class<NestedRuntimeException> supportedType() {
        return NestedRuntimeException.class;
    }

    @Override
    public KrobKrongException map(NestedRuntimeException exception) {
        SQLException sql = findSqlException(exception);
        if (sql == null || sql.getSQLState() == null) {
            return null;
        }
        return switch (sql.getSQLState()) {
            case INSUFFICIENT_PRIVILEGE ->
                new KrobKrongException(
                        CommonErrorCode.FORBIDDEN, guardMessage(sql, CommonErrorCode.FORBIDDEN.defaultMessage()));
            case CHECK_VIOLATION, RAISE_EXCEPTION ->
                new KrobKrongException(
                        CommonErrorCode.BAD_REQUEST, guardMessage(sql, "The request violates a data rule"));
            case UNIQUE_VIOLATION, EXCLUSION_VIOLATION ->
                new KrobKrongException(CommonErrorCode.CONFLICT, "The request conflicts with an existing record");
            case FOREIGN_KEY_VIOLATION ->
                new KrobKrongException(CommonErrorCode.BAD_REQUEST, "A referenced record does not exist");
            default -> null;
        };
    }

    @Override
    public int order() {
        return 100;
    }

    private static SQLException findSqlException(Throwable throwable) {
        for (Throwable t = throwable; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql) {
                return sql;
            }
        }
        return null;
    }

    // Guard RAISE messages are written for users; engine messages name tables and constraints, so they are hidden.
    private static String guardMessage(SQLException sql, String fallback) {
        if (sql instanceof PSQLException psql) {
            ServerErrorMessage server = psql.getServerErrorMessage();
            if (server != null
                    && server.getMessage() != null
                    && server.getWhere() != null
                    && server.getWhere().contains("at RAISE")) {
                return server.getMessage();
            }
        }
        return fallback;
    }
}
