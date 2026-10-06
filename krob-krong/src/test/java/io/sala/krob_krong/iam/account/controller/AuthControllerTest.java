package io.sala.krob_krong.iam.account.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.sala.krob_krong.account.controller.AuthController;
import io.sala.krob_krong.account.dto.AuthResponse;
import io.sala.krob_krong.account.service.AuthService;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.iam.security.config.SecurityConfig;
import io.sala.krob_krong.iam.security.jwt.UserJwtAuthenticationConverter;
import io.sala.krob_krong.iam.security.web.AccessDeniedExceptionMapper;
import io.sala.krob_krong.iam.security.web.AuthenticationExceptionMapper;
import io.sala.krob_krong.iam.security.web.RestAccessDeniedHandler;
import io.sala.krob_krong.iam.security.web.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({
    AuthenticationExceptionMapper.class,
    AccessDeniedExceptionMapper.class,
    SecurityConfig.class,
    UserJwtAuthenticationConverter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class
})
class AuthControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService auth;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void registrationIsPublicAndUsesServerObservedMetadata() throws Exception {
        when(auth.register(any(), any(), any()))
                .thenReturn(AuthResponse.builder()
                        .tokenType("Bearer")
                        .accessToken("access")
                        .refreshToken("refresh")
                        .expiresIn(900)
                        .build());
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "test-client")
                        .header("X-Forwarded-For", "spoofed")
                        .content("""
                    {"email":"person@example.com","display_name":"Normal Person","password":"long test passphrase"}
                    """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                .andExpect(jsonPath("$.data.access_token").value("access"))
                .andExpect(jsonPath("$.data.refresh_token").value("refresh"))
                .andExpect(jsonPath("$.data.expires_in").value(900))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.request_id").isNotEmpty())
                .andExpect(jsonPath("$.*", org.hamcrest.Matchers.hasSize(5)))
                .andExpect(jsonPath("$.http_status").doesNotExist());
        verify(auth).register(any(), eq("test-client"), eq("127.0.0.1"));
    }

    @Test
    void loginAndRefreshDoNotRequireAccessToken() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"person@example.com\",\"password\":\"long test passphrase\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"opaque-token\"}"))
                .andExpect(status().isOk());
        verify(auth).login(any(), any(), eq("127.0.0.1"));
        verify(auth).refresh(eq("opaque-token"), any(), eq("127.0.0.1"));
    }

    @Test
    void invalidInputsReturnFieldErrorsWithoutSecrets() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid\",\"display_name\":\" \",\"password\":\"secret-short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.details.fields.email").exists())
                .andExpect(jsonPath("$.error.details.fields.display_name").exists())
                .andExpect(jsonPath("$.error.details.fields.password").exists())
                .andExpect(content().string(not(containsString("secret-short"))));
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(auth);
    }

    @Test
    void malformedJsonDoesNotEchoSecrets() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"do-not-echo-this-secret\",\"email\":}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Malformed request body"))
                .andExpect(content().string(not(containsString("do-not-echo-this-secret"))));
        verifyNoInteractions(auth);
    }

    @Test
    void authenticationFailuresUseStandardEnvelope() throws Exception {
        when(auth.login(any(), any(), any())).thenThrow(new BusinessException(IAMErrorCode.INVALID_CREDENTIALS));
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"person@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void otherApiRoutesRemainProtected() throws Exception {
        mvc.perform(get("/api/roles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/private")).andExpect(status().isUnauthorized());
    }
}
