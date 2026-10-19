package io.sala.krob_krong.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.sala.krob_krong.account.dto.MfaStatusResponse;
import io.sala.krob_krong.account.dto.TotpEnrollmentResponse;
import io.sala.krob_krong.account.service.MfaService;
import io.sala.krob_krong.iam.security.AuthenticatedUser;
import io.sala.krob_krong.iam.security.config.SecurityConfig;
import io.sala.krob_krong.iam.security.jwt.UserAuthenticationToken;
import io.sala.krob_krong.iam.security.jwt.UserJwtAuthenticationConverter;
import io.sala.krob_krong.iam.security.web.AccessDeniedExceptionMapper;
import io.sala.krob_krong.iam.security.web.AuthenticationExceptionMapper;
import io.sala.krob_krong.iam.security.web.RestAccessDeniedHandler;
import io.sala.krob_krong.iam.security.web.RestAuthenticationEntryPoint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MfaController.class)
@Import({
    AuthenticationExceptionMapper.class,
    AccessDeniedExceptionMapper.class,
    SecurityConfig.class,
    UserJwtAuthenticationConverter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class
})
class MfaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MfaService mfa;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final UUID userId = UUID.randomUUID();

    @Test
    void enrollmentRequiresSignIn() throws Exception {
        mvc.perform(post("/api/me/mfa/totp")).andExpect(status().isUnauthorized());
        verifyNoInteractions(mfa);
    }

    @Test
    void enrollmentReturnsTheSecretOnceWithoutCaching() throws Exception {
        when(mfa.startTotpEnrollment(userId))
                .thenReturn(new TotpEnrollmentResponse("SECRET", "otpauth://totp/Krob%20Krong?secret=SECRET"));

        mvc.perform(post("/api/me/mfa/totp").with(authentication(signedIn())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.data.secret").value("SECRET"))
                .andExpect(jsonPath("$.data.otpauth_uri").value("otpauth://totp/Krob%20Krong?secret=SECRET"));
    }

    @Test
    void activationTakesASixDigitCode() throws Exception {
        Instant enabledAt = Instant.parse("2026-10-19T00:00:00Z");
        when(mfa.activateTotp(userId, "123456")).thenReturn(new MfaStatusResponse(enabledAt));

        mvc.perform(post("/api/me/mfa/totp/activate")
                        .with(authentication(signedIn()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mfa_enabled_at").value("2026-10-19T00:00:00Z"));
        verify(mfa).activateTotp(userId, "123456");
    }

    @Test
    void malformedCodeIsRejectedBeforeTheService() throws Exception {
        mvc.perform(post("/api/me/mfa/totp/activate")
                        .with(authentication(signedIn()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"12345a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.fields.code").exists());
        verify(mfa, never()).activateTotp(any(), any());
    }

    private UserAuthenticationToken signedIn() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .build();
        AuthenticatedUser user =
                new AuthenticatedUser(userId, UUID.randomUUID(), AuthenticatedUser.KIND_PERSON, null, null, false);
        return new UserAuthenticationToken(jwt, user, List.of());
    }
}
