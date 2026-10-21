package io.sala.krob_krong.account.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sala.krob_krong.account.dto.TotpEnrollmentResponse;
import io.sala.krob_krong.account.entity.MfaTotpEntity;
import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.account.mfa.MfaProperties;
import io.sala.krob_krong.account.mfa.Totp;
import io.sala.krob_krong.account.mfa.TotpSecretCipher;
import io.sala.krob_krong.account.repository.MfaTotpRepository;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.common.exceptions.KrobKrongException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MfaServiceImplTest {

    private final UserRepository users = mock(UserRepository.class);
    private final MfaTotpRepository totps = mock(MfaTotpRepository.class);
    private final MfaProperties props = new MfaProperties();
    private final AtomicReference<MfaTotpEntity> stored = new AtomicReference<>();
    private final UserEntity user = new UserEntity();
    private TotpSecretCipher cipher;
    private MfaServiceImpl mfa;

    @BeforeEach
    void setUp() {
        props.setEncryptionKey("test-only-mfa-encryption-key-0123456789");
        props.setMaxFailedAttempts(3);
        cipher = new TotpSecretCipher(props);
        mfa = new MfaServiceImpl(users, totps, cipher, props);

        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(totps.findById(user.getId())).thenAnswer(_ -> Optional.ofNullable(stored.get()));
        when(totps.findByIdForUpdate(user.getId())).thenAnswer(_ -> Optional.ofNullable(stored.get()));
        when(totps.save(any())).thenAnswer(call -> {
            stored.set(call.getArgument(0));
            return call.getArgument(0);
        });
    }

    @Test
    void enrollmentStoresOnlyTheEncryptedSecret() {
        TotpEnrollmentResponse response = mfa.startTotpEnrollment(user.getId());

        MfaTotpEntity totp = stored.get();
        assertThat(totp.isActive()).isFalse();
        assertThat(totp.getSecretCiphertext()).doesNotContain(response.getSecret());
        assertThat(Totp.base32(cipher.decrypt(user.getId(), totp.getSecretCiphertext())))
                .isEqualTo(response.getSecret());
        assertThat(response.getOtpauthUri())
                .startsWith("otpauth://totp/Krob%20Krong%3Aadmin%40example.com?secret=" + response.getSecret());
        assertThat(user.getMfaEnabledAt()).isNull();
    }

    @Test
    void enrollmentIsRefusedOnceMfaIsEnabled() {
        user.setMfaEnabledAt(Instant.now());

        assertError(() -> mfa.startTotpEnrollment(user.getId()), "MFA_ALREADY_ENABLED");
        verify(totps, never()).save(any());
    }

    @Test
    void activationWithACurrentCodeEnablesMfa() {
        mfa.startTotpEnrollment(user.getId());
        long step = Totp.step(Instant.now());
        byte[] secret = cipher.decrypt(user.getId(), stored.get().getSecretCiphertext());

        mfa.activateTotp(user.getId(), Totp.code(secret, step));

        assertThat(user.getMfaEnabledAt()).isNotNull();
        assertThat(stored.get().isActive()).isTrue();
        assertThat(stored.get().getLastUsedStep()).isEqualTo(step);
    }

    @Test
    void activationNeedsAnEnrollment() {
        assertError(() -> mfa.activateTotp(user.getId(), "123456"), "MFA_ENROLLMENT_NOT_FOUND");
    }

    @Test
    void loginCodeCannotBeReplayed() {
        mfa.startTotpEnrollment(user.getId());
        String code = currentCode();
        mfa.activateTotp(user.getId(), code);

        assertError(() -> mfa.verifyLoginCode(user.getId(), code), "INVALID_MFA_CODE");
    }

    @Test
    void loginCodeIsRefusedForAPendingEnrollment() {
        mfa.startTotpEnrollment(user.getId());

        assertError(() -> mfa.verifyLoginCode(user.getId(), currentCode()), "INVALID_MFA_CODE");
    }

    @Test
    void repeatedWrongCodesLockTheFactor() {
        mfa.startTotpEnrollment(user.getId());
        String wrong = wrongCode();

        assertError(() -> mfa.activateTotp(user.getId(), wrong), "INVALID_MFA_CODE");
        assertError(() -> mfa.activateTotp(user.getId(), wrong), "INVALID_MFA_CODE");
        assertThat(stored.get().getFailedAttempts()).isEqualTo(2);
        assertError(() -> mfa.activateTotp(user.getId(), wrong), "INVALID_MFA_CODE");

        assertThat(stored.get().getLockedUntil()).isAfter(Instant.now());
        assertError(() -> mfa.activateTotp(user.getId(), currentCode()), "MFA_LOCKED");
        assertThat(user.getMfaEnabledAt()).isNull();
    }

    @Test
    void aCorrectCodeClearsEarlierFailures() {
        mfa.startTotpEnrollment(user.getId());
        assertError(() -> mfa.activateTotp(user.getId(), wrongCode()), "INVALID_MFA_CODE");

        mfa.activateTotp(user.getId(), currentCode());

        assertThat(stored.get().getFailedAttempts()).isZero();
    }

    private String currentCode() {
        byte[] secret = cipher.decrypt(user.getId(), stored.get().getSecretCiphertext());
        return Totp.code(secret, Totp.step(Instant.now()));
    }

    private String wrongCode() {
        byte[] secret = cipher.decrypt(user.getId(), stored.get().getSecretCiphertext());
        long step = Totp.step(Instant.now());
        for (int candidate = 0; ; candidate++) {
            String code = "%06d".formatted(candidate);
            if (!code.equals(Totp.code(secret, step - 1))
                    && !code.equals(Totp.code(secret, step))
                    && !code.equals(Totp.code(secret, step + 1))) {
                return code;
            }
        }
    }

    private static void assertError(ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(KrobKrongException.class, ex -> assertThat(ex.errorCode())
                .isEqualTo(code));
    }
}
