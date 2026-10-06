package io.sala.krob_krong.iam.account.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.sala.krob_krong.account.web.AccountConstraintExceptionMapper;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;

class AccountConstraintExceptionMapperTest {
    private final AccountConstraintExceptionMapper mapper = new AccountConstraintExceptionMapper();

    @Test
    void emailUniqueViolationMapsToRegistrationConflictWithoutDatabaseDetails() {
        var result = mapper.map(violation("app_user_email_uq"));
        assertThat(result.errorCode()).isEqualTo("EMAIL_ALREADY_REGISTERED");
        assertThat(result.httpStatus()).isEqualTo(409);
        assertThat(result.getMessage()).doesNotContain("app_user_email_uq");
    }

    @Test
    void unrelatedConstraintsAndErrorsAreLeftToOtherMappers() {
        assertThat(mapper.map(violation("refresh_token_token_hash_key"))).isNull();
        assertThat(mapper.map(new DataIntegrityViolationException("unrelated"))).isNull();
    }

    private static DataIntegrityViolationException violation(String constraint) {
        var sql = new PSQLException(new ServerErrorMessage("SERROR\0C23505\0Mduplicate value\0n" + constraint + "\0"));
        return new DataIntegrityViolationException("save failed", sql);
    }
}
