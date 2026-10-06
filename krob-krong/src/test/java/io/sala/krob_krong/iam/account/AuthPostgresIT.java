package io.sala.krob_krong.iam.account;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.sala.krob_krong.account.dto.AuthResponse;
import io.sala.krob_krong.account.dto.LoginRequest;
import io.sala.krob_krong.account.dto.RegisterRequest;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.account.service.AuthService;
import io.sala.krob_krong.common.exceptions.KrobKrongException;
import io.sala.krob_krong.iam.security.impl.TokenServiceImpl;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

// Point AUTH_TEST_DB_URL at a disposable database: this test applies all migrations and writes test accounts.
@SpringBootTest(
        properties = {"spring.datasource.url=${AUTH_TEST_DB_URL}", "krob-krong.storage.s3.auto-create-bucket=false"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "AUTH_IT", matches = "true")
class AuthPostgresIT {
    @MockitoSpyBean
    private TokenServiceImpl tokenService;

    private static final String PASSWORD = "a sufficiently long passphrase";

    @Autowired
    private AuthService auth;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwords;

    @Autowired
    private JwtDecoder jwt;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void publicHttpFlowRegistersLogsInAndRotatesWithoutPrivilegeEscalation() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"email":"%s","password":"%s","display_name":" Person ",
                     "kind":"service","verifiedAt":"2026-01-01T00:00:00Z","mfaVerified":true,"role":"super_admin"}
                    """.formatted(email.toUpperCase(java.util.Locale.ROOT), PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        AuthResponse registered = json.treeToValue(json.readTree(body).get("data"), AuthResponse.class);
        var user = users.findById(registered.getUserId()).orElseThrow();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getDisplayName()).isEqualTo("Person");
        assertThat(user.isPerson()).isTrue();
        assertThat(user.isActive()).isTrue();
        assertThat(user.getVerifiedAt()).isNull();
        assertThat(user.getMfaEnabledAt()).isNull();
        assertThat(user.getPasswordHash()).doesNotContain(PASSWORD);
        assertThat(passwords.matches(PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(jwt.decode(registered.getAccessToken()).getClaimAsBoolean("mfa"))
                .isFalse();
        assertThat(jdbc.sql("SELECT count(*) FROM user_role WHERE user_id = ?")
                        .param(user.getId())
                        .query(Long.class)
                        .single())
                .isZero();
        mvc.perform(get("/api/roles").header("Authorization", "Bearer " + registered.getAccessToken()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").isNotEmpty());
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"%s\"}".formatted(registered.getRefreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refresh_token").isNotEmpty());
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\",\"display_name\":\"Again\"}"
                                .formatted(email, PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void reuseRevokesTheWholeFamilyButLeavesOtherLoginSessionsWorking() {
        AuthResponse original = register();
        AuthResponse independent = auth.login(login(original.getEmail(), PASSWORD), null, null);
        AuthResponse rotated = auth.refresh(original.getRefreshToken(), null, null);
        assertThat(rotated.getRefreshToken()).isNotEqualTo(original.getRefreshToken());
        assertThat(jwt.decode(rotated.getAccessToken()).getClaimAsString("sid"))
                .isEqualTo(jwt.decode(original.getAccessToken()).getClaimAsString("sid"));
        assertError(() -> auth.refresh(original.getRefreshToken(), null, null), "REFRESH_TOKEN_REUSE");
        assertThatThrownBy(() -> jwt.decode(rotated.getAccessToken())).isInstanceOf(JwtException.class);
        assertError(() -> auth.refresh(rotated.getRefreshToken(), null, null), "REFRESH_TOKEN_REUSE");
        assertThat(jwt.decode(independent.getAccessToken()).getSubject())
                .isEqualTo(original.getUserId().toString());
    }

    @Test
    void concurrentRefreshesProduceOnlyOneSuccessAndRevokeItsSession() throws Exception {
        AuthResponse original = register();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Object> refresh = () -> {
                start.await();
                try {
                    return auth.refresh(original.getRefreshToken(), null, null);
                } catch (KrobKrongException ex) {
                    return ex.errorCode();
                }
            };
            var first = executor.submit(refresh);
            var second = executor.submit(refresh);
            start.countDown();
            List<Object> results = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertThat(results.stream().filter(AuthResponse.class::isInstance).count())
                    .isEqualTo(1);
            assertThat(results).contains("REFRESH_TOKEN_REUSE");
            var winner = results.stream()
                    .filter(AuthResponse.class::isInstance)
                    .map(AuthResponse.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertThatThrownBy(() -> jwt.decode(winner.getAccessToken())).isInstanceOf(JwtException.class);
            assertThat(activeTokens(original.getUserId())).isZero();
        }
    }

    @Test
    void invalidCredentialsExpiredTokensSuspensionAndMfaDoNotIssueTokens() {
        AuthResponse registered = register();
        assertError(() -> auth.login(login(registered.getEmail(), "wrong"), null, null), "INVALID_CREDENTIALS");
        assertError(() -> auth.login(login("absent@example.com", PASSWORD), null, null), "INVALID_CREDENTIALS");
        assertError(() -> auth.refresh("unknown-token", null, null), "INVALID_REFRESH_TOKEN");
        jdbc.sql("UPDATE app_user SET is_active = false WHERE id = ?")
                .param(registered.getUserId())
                .update();
        assertError(() -> auth.login(login(registered.getEmail(), PASSWORD), null, null), "ACCOUNT_SUSPENDED");
        assertError(() -> auth.refresh(registered.getRefreshToken(), null, null), "ACCOUNT_SUSPENDED");
        assertThatThrownBy(() -> jwt.decode(registered.getAccessToken())).isInstanceOf(JwtException.class);
        jdbc.sql("UPDATE app_user SET is_active = true, mfa_enabled_at = now() WHERE id = ?")
                .param(registered.getUserId())
                .update();
        assertError(() -> auth.login(login(registered.getEmail(), PASSWORD), null, null), "MFA_REQUIRED");
        assertError(() -> auth.refresh(registered.getRefreshToken(), null, null), "MFA_REQUIRED");
        jdbc.sql("UPDATE app_user SET mfa_enabled_at = null WHERE id = ?")
                .param(registered.getUserId())
                .update();
        jdbc.sql("UPDATE refresh_token SET expires_at = now() - interval '1 minute' WHERE user_id = ?")
                .param(registered.getUserId())
                .update();
        assertError(() -> auth.refresh(registered.getRefreshToken(), null, null), "INVALID_REFRESH_TOKEN");
        assertThat(activeTokens(registered.getUserId())).isZero();
    }

    @Test
    void serviceAccountsAndAccountsWithoutPasswordCannotLogIn() {
        AuthResponse registered = register();
        jdbc.sql("UPDATE app_user SET kind = 'service' WHERE id = ?")
                .param(registered.getUserId())
                .update();
        assertError(() -> auth.login(login(registered.getEmail(), PASSWORD), null, null), "INVALID_CREDENTIALS");
        assertError(() -> auth.refresh(registered.getRefreshToken(), null, null), "INVALID_CREDENTIALS");
        jdbc.sql("UPDATE app_user SET kind = 'person', password_hash = null WHERE id = ?")
                .param(registered.getUserId())
                .update();
        assertError(() -> auth.login(login(registered.getEmail(), PASSWORD), null, null), "INVALID_CREDENTIALS");
    }

    @Test
    void tokenSigningFailureRollsBackRegistrationAndRefresh() {
        AuthResponse original = register();
        String failedEmail = UUID.randomUUID() + "@example.com";
        RegisterRequest request = new RegisterRequest();
        request.setEmail(failedEmail);
        request.setDisplayName("Person");
        request.setPassword(PASSWORD);
        doThrow(new IllegalStateException("signing unavailable"))
                .when(tokenService)
                .issueAccessToken(any());
        try {
            assertThatThrownBy(() -> auth.register(request, null, null)).isInstanceOf(IllegalStateException.class);
            assertThat(jdbc.sql("SELECT count(*) FROM app_user WHERE email = ?")
                            .param(failedEmail)
                            .query(Long.class)
                            .single())
                    .isZero();
            assertThatThrownBy(() -> auth.refresh(original.getRefreshToken(), null, null))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(activeTokens(original.getUserId())).isEqualTo(1);
        } finally {
            doCallRealMethod().when(tokenService).issueAccessToken(any());
        }
        assertThat(auth.refresh(original.getRefreshToken(), null, null).getAccessToken())
                .isNotEmpty();
    }

    @Test
    void concurrentRegistrationReturnsOneCreatedAndOneEmailConflict() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String body = "{\"email\":\"%s\",\"password\":\"%s\",\"display_name\":\"Person\"}".formatted(email, PASSWORD);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Integer> register = () -> {
                start.await();
                var response = mvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andReturn()
                        .getResponse();
                if (response.getStatus() == 409) {
                    assertThat(json.readTree(response.getContentAsString())
                                    .path("error")
                                    .path("code")
                                    .asText())
                            .isEqualTo("EMAIL_ALREADY_REGISTERED");
                }
                return response.getStatus();
            };
            var first = executor.submit(register);
            var second = executor.submit(register);
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(jdbc.sql("SELECT count(*) FROM app_user WHERE email = ?")
                            .param(email)
                            .query(Long.class)
                            .single())
                    .isEqualTo(1);
        }
    }

    private long activeTokens(UUID userId) {
        return jdbc.sql(
                        "SELECT count(*) FROM refresh_token WHERE user_id = ? AND revoked_at IS NULL AND expires_at > now()")
                .param(userId)
                .query(Long.class)
                .single();
    }

    private AuthResponse register() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(UUID.randomUUID() + "@example.com");
        request.setDisplayName("Person");
        request.setPassword(PASSWORD);
        return auth.register(request, "test-client", "127.0.0.1");
    }

    private static LoginRequest login(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(KrobKrongException.class, ex -> assertThat(ex.errorCode())
                .isEqualTo(code));
    }
}
