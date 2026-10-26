package io.sala.krob_krong.account.mfa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TotpSecretCipherTest {

    private final TotpSecretCipher cipher = new TotpSecretCipher(props("test-only-mfa-encryption-key-0123456789"));

    @Test
    void roundTripsWithAFreshNoncePerEncryption() {
        UUID userId = UUID.randomUUID();
        byte[] secret = Totp.newSecret();

        String first = cipher.encrypt(userId, secret);
        String second = cipher.encrypt(userId, secret);

        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(userId, first)).isEqualTo(secret);
        assertThat(cipher.decrypt(userId, second)).isEqualTo(secret);
    }

    @Test
    void ciphertextIsBoundToItsUser() {
        String sealed = cipher.encrypt(UUID.randomUUID(), Totp.newSecret());

        assertThatThrownBy(() -> cipher.decrypt(UUID.randomUUID(), sealed)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anotherKeyCannotDecrypt() {
        UUID userId = UUID.randomUUID();
        String sealed = cipher.encrypt(userId, Totp.newSecret());
        TotpSecretCipher other = new TotpSecretCipher(props("another-test-mfa-encryption-key-987654321"));

        assertThatThrownBy(() -> other.decrypt(userId, sealed)).isInstanceOf(IllegalStateException.class);
    }

    private static MfaProperties props(String key) {
        MfaProperties props = new MfaProperties();
        props.setEncryptionKey(key);
        return props;
    }
}
