package io.sala.krob_krong.iam.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sala.krob_krong.account.repository.RefreshTokenRepository;
import io.sala.krob_krong.iam.security.config.JwtConfig;
import io.sala.krob_krong.iam.security.config.JwtProperties;
import io.sala.krob_krong.iam.security.dto.SessionClaims;
import io.sala.krob_krong.iam.security.impl.TokenServiceImpl;
import io.sala.krob_krong.iam.security.jwt.ActiveSessionValidator;
import io.sala.krob_krong.iam.security.jwt.UserAuthenticationToken;
import io.sala.krob_krong.iam.security.jwt.UserJwtAuthenticationConverter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class JwtTokenRoundTripTest {

    private final JwtConfig jwtConfig = new JwtConfig();
    private final ActiveSessionValidator sessionValidator = mock(ActiveSessionValidator.class);
    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    private JwtProperties props;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        props = properties("test-signing-key-0123456789-0123456789");
        when(sessionValidator.validate(any(Jwt.class))).thenReturn(OAuth2TokenValidatorResult.success());
        decoder = jwtConfig.jwtDecoder(props, sessionValidator);
    }

    @Test
    void issuedAccessTokenDecodesToAuthenticatedUser() {
        Jwt jwt = decoder.decode(issue(props));

        UserAuthenticationToken authentication =
                (UserAuthenticationToken) new UserJwtAuthenticationConverter().convert(jwt);

        AuthenticatedUser user = authentication.getPrincipal();
        assertThat(user.id()).isEqualTo(userId);
        assertThat(user.sessionId()).isEqualTo(sessionId);
        assertThat(user.isPerson()).isTrue();
        assertThat(user.mfaVerified()).isTrue();
        assertThat(authentication.getName()).isEqualTo(userId.toString());
        assertThat(jwt.getHeaders()).containsEntry("typ", "at+jwt");
        assertThat(jwt.getClaims()).doesNotContainKeys("permissions", "roles", "tenant", "platform_admin");
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String forged = issue(properties("attacker-signing-key-0123456789-01234567"));

        assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void rejectsTokenIssuedForAnotherAudience() {
        JwtProperties otherAudience = properties(props.getSecret());
        otherAudience.setAudience("some-other-api");

        assertThatThrownBy(() -> decoder.decode(issue(otherAudience))).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rejectsExpiredToken() {
        Instant issuedAt = Instant.now().minus(Duration.ofMinutes(20));
        String token = encode("at+jwt", issuedAt, issuedAt.plus(Duration.ofMinutes(15)));

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rejectsSameKeyJwtThatIsNotAnAccessToken() {
        Instant now = Instant.now();
        String token = encode("JWT", now, now.plus(Duration.ofMinutes(5)));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    private String encode(String type, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.getIssuer())
                .audience(List.of(props.getAudience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(userId.toString())
                .claim("sid", sessionId.toString())
                .claim("kind", AuthenticatedUser.KIND_PERSON)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type(type).build();
        return jwtConfig
                .jwtEncoder(props)
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    @Test
    void rejectsTokenWhoseSessionWasRevoked() {
        when(sessionValidator.validate(any(Jwt.class)))
                .thenReturn(
                        OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "session ended", null)));

        assertThatThrownBy(() -> decoder.decode(issue(props)))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("session ended");
    }

    private String issue(JwtProperties issuerProps) {
        TokenServiceImpl tokenService = new TokenServiceImpl(
                jwtConfig.jwtEncoder(issuerProps), issuerProps, mock(RefreshTokenRepository.class));
        SessionClaims claims = SessionClaims.builder()
                .userId(userId)
                .sessionId(sessionId)
                .kind(AuthenticatedUser.KIND_PERSON)
                .email("owner@example.com")
                .displayName("Owner")
                .mfaVerified(true)
                .build();
        return tokenService.issueAccessToken(claims).getValue();
    }

    private static JwtProperties properties(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        return properties;
    }
}
