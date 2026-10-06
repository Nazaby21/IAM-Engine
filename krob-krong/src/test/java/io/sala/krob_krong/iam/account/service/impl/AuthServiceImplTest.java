package io.sala.krob_krong.iam.account.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.sala.krob_krong.account.dto.LoginRequest;
import io.sala.krob_krong.account.dto.RegisterRequest;
import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.account.service.AuthSessionService;
import io.sala.krob_krong.account.service.impl.AuthServiceImpl;
import io.sala.krob_krong.common.exceptions.KrobKrongException;
import io.sala.krob_krong.iam.security.TokenService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceImplTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final AuthSessionService sessions = mock(AuthSessionService.class);
    private final TokenService tokens = mock(TokenService.class);
    private AuthServiceImpl auth;

    @BeforeEach
    void setUp() {
        when(passwords.encode(anyString())).thenReturn("dummy-hash");
        auth = new AuthServiceImpl(users, passwords, sessions, tokens);
    }

    @Test
    void registrationHashesPasswordAndKeepsPrivilegesServerOwned() {
        when(passwords.encode("long test passphrase")).thenReturn("real-hash");
        RegisterRequest request = new RegisterRequest();
        request.setEmail("  PERSON@EXAMPLE.COM  ");
        request.setDisplayName("  Person  ");
        request.setPassword("long test passphrase");
        auth.register(request, "browser", "127.0.0.1");
        var user = ArgumentCaptor.forClass(UserEntity.class);
        verify(sessions).register(user.capture(), eq("browser"), eq("127.0.0.1"));
        assertThat(user.getValue().getEmail()).isEqualTo("person@example.com");
        assertThat(user.getValue().getDisplayName()).isEqualTo("Person");
        assertThat(user.getValue().getPasswordHash()).isEqualTo("real-hash");
        assertThat(user.getValue().isPerson()).isTrue();
        assertThat(user.getValue().isActive()).isTrue();
        assertThat(user.getValue().getVerifiedAt()).isNull();
        assertThat(user.getValue().getMfaEnabledAt()).isNull();
    }

    @Test
    void duplicateEmailDoesNotCreateASession() {
        when(users.exists(any(Specification.class))).thenReturn(true);
        RegisterRequest request = new RegisterRequest();
        request.setEmail("person@example.com");
        assertError(() -> auth.register(request, null, null), "EMAIL_ALREADY_REGISTERED");
        verifyNoInteractions(sessions);
    }

    @Test
    void unknownAccountRunsDummyHashVerificationAndReturnsGenericError() {
        when(users.findOne(any(Specification.class))).thenReturn(Optional.empty());
        assertError(() -> auth.login(login(), null, null), "INVALID_CREDENTIALS");
        verify(passwords).matches("secret", "dummy-hash");
        verifyNoInteractions(sessions);
    }

    @Test
    void serviceAccountsAndCredentiallessAccountsCannotSignIn() {
        UserEntity user = new UserEntity();
        user.setKind("service");
        user.setPasswordHash("hash");
        when(users.findOne(any(Specification.class))).thenReturn(Optional.of(user));
        when(passwords.matches("secret", "hash")).thenReturn(true);
        assertError(() -> auth.login(login(), null, null), "INVALID_CREDENTIALS");
        user.setKind("person");
        user.setPasswordHash(null);
        when(passwords.matches("secret", "dummy-hash")).thenReturn(true);
        assertError(() -> auth.login(login(), null, null), "INVALID_CREDENTIALS");
        verifyNoInteractions(sessions);
    }

    @Test
    void validPasswordStartsSessionForTheStoredUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setPasswordHash("hash");
        when(users.findOne(any(Specification.class))).thenReturn(Optional.of(user));
        when(passwords.matches("secret", "hash")).thenReturn(true);
        auth.login(login(), null, null);
        verify(sessions).login(user.getId(), null, null);
    }

    private static LoginRequest login() {
        LoginRequest request = new LoginRequest();
        request.setEmail("person@example.com");
        request.setPassword("secret");
        return request;
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(KrobKrongException.class, ex -> assertThat(ex.errorCode())
                .isEqualTo(code));
    }
}
