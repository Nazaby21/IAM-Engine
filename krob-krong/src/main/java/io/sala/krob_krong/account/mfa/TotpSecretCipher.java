package io.sala.krob_krong.account.mfa;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(MfaProperties.class)
public class TotpSecretCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    public TotpSecretCipher(MfaProperties props) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(props.getEncryptionKey().getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(digest, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public String encrypt(UUID userId, byte[] secret) {
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(userId));
            byte[] sealed = cipher.doFinal(secret);
            return Base64.getEncoder()
                    .encodeToString(ByteBuffer.allocate(iv.length + sealed.length)
                            .put(iv)
                            .put(sealed)
                            .array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt the TOTP secret", e);
        }
    }

    public byte[] decrypt(UUID userId, String ciphertext) {
        try {
            byte[] stored = Base64.getDecoder().decode(ciphertext);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, stored, 0, IV_BYTES));
            cipher.updateAAD(aad(userId));
            return cipher.doFinal(stored, IV_BYTES, stored.length - IV_BYTES);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Could not decrypt the TOTP secret", e);
        }
    }

    private static byte[] aad(UUID userId) {
        return userId.toString().getBytes(StandardCharsets.UTF_8);
    }
}
