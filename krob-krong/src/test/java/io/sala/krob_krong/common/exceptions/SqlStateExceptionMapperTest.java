package io.sala.krob_krong.common.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;

class SqlStateExceptionMapperTest {

    private static final String GUARD_WHERE = "PL/pgSQL function user_role_guard() line 112 at RAISE";

    private final SqlStateExceptionMapper mapper = new SqlStateExceptionMapper();

    @Test
    void guardDenialBecomes403WithGuardMessage() {
        KrobKrongException mapped =
                mapper.map(dbError("42501", "user may not grant role school_owner here", GUARD_WHERE));

        assertThat(mapped.httpStatus()).isEqualTo(403);
        assertThat(mapped.getMessage()).isEqualTo("user may not grant role school_owner here");
    }

    @Test
    void guardRuleViolationBecomes400WithGuardMessage() {
        String message = "a school must keep at least one owner; add another owner first";

        KrobKrongException mapped = mapper.map(dbError("23514", message, GUARD_WHERE));

        assertThat(mapped.httpStatus()).isEqualTo(400);
        assertThat(mapped.getMessage()).isEqualTo(message);
    }

    @Test
    void engineMessagesAreNotExposed() {
        KrobKrongException check = mapper.map(
                dbError("23514", "new row for relation \"school\" violates check constraint \"school_check\"", null));
        KrobKrongException privilege = mapper.map(dbError("42501", "permission denied for table app_user", null));

        assertThat(check.getMessage()).isEqualTo("The request violates a data rule");
        assertThat(privilege.httpStatus()).isEqualTo(403);
        assertThat(privilege.getMessage()).doesNotContain("app_user");
    }

    @Test
    void overlappingGrantBecomes409() {
        KrobKrongException mapped =
                mapper.map(dbError("23P01", "conflicting key value violates exclusion constraint", null));

        assertThat(mapped.httpStatus()).isEqualTo(409);
    }

    @Test
    void exceptionsWithoutSqlStateAreLeftToOtherHandlers() {
        assertThat(mapper.map(new DataRetrievalFailureException("not found"))).isNull();
    }

    private static DataIntegrityViolationException dbError(String sqlState, String message, String where) {
        String fields = "SERROR\0C" + sqlState + "\0M" + message + (where != null ? "\0W" + where : "") + "\0";
        PSQLException psql = new PSQLException(new ServerErrorMessage(fields));
        return new DataIntegrityViolationException("could not execute statement", new RuntimeException(psql));
    }
}
