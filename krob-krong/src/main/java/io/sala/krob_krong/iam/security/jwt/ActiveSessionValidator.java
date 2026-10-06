package io.sala.krob_krong.iam.security.jwt;

import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

// Makes logout, refresh-token reuse detection and account suspension take effect before the access token expires.
@Component
public class ActiveSessionValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error SESSION_ENDED =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The session has ended or the account is inactive", null);

    private static final String ACTIVE_SESSION_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM app_user u
                JOIN refresh_token rt ON rt.user_id = u.id
                WHERE u.id = :userId
                  AND u.is_active
                  AND rt.family_id = :sessionId
                  AND rt.revoked_at IS NULL
                  AND rt.expires_at > now())
            """;

    private final JdbcClient jdbc;

    public ActiveSessionValidator(DataSource dataSource) {
        this.jdbc = JdbcClient.create(dataSource);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        UUID userId = parseUuid(jwt.getSubject());
        UUID sessionId = parseUuid(jwt.getClaimAsString(TokenClaims.SESSION_ID));
        if (userId == null || sessionId == null) {
            return OAuth2TokenValidatorResult.failure(SESSION_ENDED);
        }
        boolean active = jdbc.sql(ACTIVE_SESSION_SQL)
                .param("userId", userId)
                .param("sessionId", sessionId)
                .query(Boolean.class)
                .single();
        return active ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(SESSION_ENDED);
    }

    private static UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
