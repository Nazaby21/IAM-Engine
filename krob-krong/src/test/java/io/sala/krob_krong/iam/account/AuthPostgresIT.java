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
import io.sala.krob_krong.account.mfa.Totp;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.account.service.AuthService;
import io.sala.krob_krong.common.exceptions.KrobKrongException;
import io.sala.krob_krong.iam.security.impl.TokenServiceImpl;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
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

    @Autowired
    private TransactionTemplate transactions;

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

    @Test
    void mfaEnrolledSuperAdminSignsInWithACodeAndReachesAdminEndpoints() throws Exception {
        AuthResponse registered = register();
        String passwordOnly = "Bearer " + registered.getAccessToken();
        Enrolled mfa = enrollMfa(registered);
        String activationCode = Totp.code(mfa.secret(), mfa.step());
        assertThat(jdbc.sql("SELECT secret_ciphertext FROM mfa_totp WHERE user_id = ?")
                        .param(registered.getUserId())
                        .query(String.class)
                        .single())
                .doesNotContain(Totp.base32(mfa.secret()));
        grantSuperAdmin(registered.getUserId(), passwordOnly);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(registered, "")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("MFA_REQUIRED"));
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(registered, activationCode)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_MFA_CODE"));
        AuthResponse admin = json.treeToValue(
                data(mvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(registered, Totp.code(mfa.secret(), mfa.step() + 1))))
                        .andExpect(status().isOk())),
                AuthResponse.class);
        assertThat(jwt.decode(admin.getAccessToken()).getClaimAsBoolean("mfa")).isTrue();

        mvc.perform(get("/api/admin/schools").header("Authorization", "Bearer " + admin.getAccessToken()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/schools").header("Authorization", passwordOnly))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("MFA_REQUIRED"));
        assertError(() -> auth.refresh(registered.getRefreshToken(), null, null), "MFA_REQUIRED");

        AuthResponse refreshed = auth.refresh(admin.getRefreshToken(), null, null);
        assertThat(jwt.decode(refreshed.getAccessToken()).getClaimAsBoolean("mfa"))
                .isTrue();
        mvc.perform(get("/api/admin/schools").header("Authorization", "Bearer " + refreshed.getAccessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void wrongCodesLockTheFactorEvenAfterTheFailedRequestEnds() throws Exception {
        AuthResponse registered = register();
        String bearer = "Bearer " + registered.getAccessToken();
        mvc.perform(post("/api/me/mfa/totp").header("Authorization", bearer)).andExpect(status().isCreated());
        for (int attempt = 1; attempt <= 5; attempt++) {
            mvc.perform(post("/api/me/mfa/totp/activate")
                    .header("Authorization", bearer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"%06d\"}".formatted(attempt)));
        }
        assertThat(jdbc.sql("SELECT locked_until > now() FROM mfa_totp WHERE user_id = ?")
                        .param(registered.getUserId())
                        .query(Boolean.class)
                        .single())
                .isTrue();
        mvc.perform(post("/api/me/mfa/totp/activate")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("MFA_LOCKED"));
    }

    @Test
    void superAdminInvitesAnotherSuperAdminThroughTheApi() throws Exception {
        AuthResponse first = register();
        Enrolled firstMfa = enrollMfa(first);
        grantSuperAdmin(first.getUserId(), "Bearer " + first.getAccessToken());
        String admin = "Bearer " + signInWithCode(first, firstMfa).getAccessToken();
        AuthResponse second = register();
        UUID superAdminRole = roleId("super_admin");

        mvc.perform(platformInvite(admin, second.getUserId(), superAdminRole))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("platform roles require MFA"));
        Enrolled secondMfa = enrollMfa(second);
        mvc.perform(platformInvite("Bearer " + first.getAccessToken(), second.getUserId(), superAdminRole))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("MFA_REQUIRED"));
        mvc.perform(platformInvite(admin, second.getUserId(), roleId("student")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ROLE_NOT_GRANTABLE_HERE"));
        JsonNode invitation = data(mvc.perform(platformInvite(admin, second.getUserId(), superAdminRole))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.school_id").isEmpty())
                .andExpect(jsonPath("$.data.granted_by").value(first.getUserId().toString())));
        mvc.perform(platformInvite(admin, second.getUserId(), superAdminRole))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROLE_ALREADY_HELD"));

        String secondAdmin = "Bearer " + signInWithCode(second, secondMfa).getAccessToken();
        mvc.perform(get("/api/admin/schools").header("Authorization", secondAdmin))
                .andExpect(status().isForbidden());
        mvc.perform(post(
                                "/api/me/invitations/{id}/accept",
                                invitation.path("id").asText())
                        .header("Authorization", secondAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"));
        mvc.perform(get("/api/admin/schools").header("Authorization", secondAdmin))
                .andExpect(status().isOk());
    }

    private record Enrolled(byte[] secret, long step) {}

    private Enrolled enrollMfa(AuthResponse account) throws Exception {
        String bearer = "Bearer " + account.getAccessToken();
        JsonNode enrollment = data(mvc.perform(post("/api/me/mfa/totp").header("Authorization", bearer))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.otpauth_uri").isNotEmpty()));
        byte[] secret = base32Decode(enrollment.path("secret").asText());
        long step = Totp.step(Instant.now());
        mvc.perform(post("/api/me/mfa/totp/activate")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\"}".formatted(Totp.code(secret, step))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mfa_enabled_at").isNotEmpty());
        return new Enrolled(secret, step);
    }

    private AuthResponse signInWithCode(AuthResponse account, Enrolled mfa) throws Exception {
        return json.treeToValue(
                data(mvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(account, Totp.code(mfa.secret(), mfa.step() + 1))))
                        .andExpect(status().isOk())),
                AuthResponse.class);
    }

    private static org.springframework.test.web.servlet.RequestBuilder platformInvite(
            String bearer, UUID userId, UUID roleId) {
        return post("/api/admin/members/invite")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user_id\":\"%s\",\"role_id\":\"%s\"}".formatted(userId, roleId));
    }

    private UUID roleId(String code) {
        return jdbc.sql("SELECT id FROM role WHERE code = ?")
                .param(code)
                .query(UUID.class)
                .single();
    }

    private void grantSuperAdmin(UUID userId, String bearer) throws Exception {
        Optional<UUID> existing = jdbc.sql("""
                        SELECT ur.user_id FROM user_role ur JOIN role r ON r.id = ur.role_id
                        WHERE r.kind = 'platform' AND ur.accepted_at IS NOT NULL AND ur.revoked_at IS NULL
                          AND tstzrange(ur.valid_from, ur.valid_to) @> now()
                        LIMIT 1""").query(UUID.class).optional();
        if (existing.isEmpty()) {
            jdbc.sql("INSERT INTO user_role (user_id, role_id, source) SELECT ?, id, 'bootstrap' FROM role"
                            + " WHERE code = 'super_admin'")
                    .param(userId)
                    .update();
            return;
        }
        UUID grantId = transactions.execute(_ -> {
            jdbc.sql("SELECT set_config('app.actor_id', ?, true)")
                    .param(existing.get().toString())
                    .query(String.class)
                    .single();
            return jdbc.sql("INSERT INTO user_role (user_id, role_id, source) SELECT ?, id, 'invite' FROM role"
                            + " WHERE code = 'super_admin' RETURNING id")
                    .param(userId)
                    .query(UUID.class)
                    .single();
        });
        mvc.perform(post("/api/me/invitations/{id}/accept", grantId).header("Authorization", bearer))
                .andExpect(status().isOk());
    }

    private JsonNode data(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString())
                .path("data");
    }

    private static String loginJson(AuthResponse account, String mfaCode) {
        return "{\"email\":\"%s\",\"password\":\"%s\",\"mfa_code\":\"%s\"}"
                .formatted(account.getEmail(), PASSWORD, mfaCode);
    }

    private static byte[] base32Decode(String value) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : value.toCharArray()) {
            buffer = (buffer << 5) | alphabet.indexOf(c);
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >>> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out.toByteArray();
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
