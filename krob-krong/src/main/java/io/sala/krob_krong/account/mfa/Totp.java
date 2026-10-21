package io.sala.krob_krong.account.mfa;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.OptionalLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Totp {

    private static final int SECRET_BYTES = 20;
    private static final long STEP_SECONDS = 30;
    private static final int WINDOW = 1;
    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    public static byte[] newSecret() {
        byte[] secret = new byte[SECRET_BYTES];
        RANDOM.nextBytes(secret);
        return secret;
    }

    public static long step(Instant at) {
        return Math.floorDiv(at.getEpochSecond(), STEP_SECONDS);
    }

    public static String code(byte[] secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash =
                    mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 unavailable", e);
        }
    }

    public static OptionalLong match(byte[] secret, String code, Instant at, Long lastUsedStep) {
        byte[] given = code.getBytes(StandardCharsets.US_ASCII);
        long current = step(at);
        for (long candidate = current - WINDOW; candidate <= current + WINDOW; candidate++) {
            if (lastUsedStep != null && candidate <= lastUsedStep) continue;
            if (MessageDigest.isEqual(code(secret, candidate).getBytes(StandardCharsets.US_ASCII), given)) {
                return OptionalLong.of(candidate);
            }
        }
        return OptionalLong.empty();
    }

    public static String base32(byte[] bytes) {
        StringBuilder out = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bits = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                out.append(BASE32[(buffer >>> (bits - 5)) & 31]);
                bits -= 5;
            }
        }
        if (bits > 0) {
            out.append(BASE32[(buffer << (5 - bits)) & 31]);
        }
        return out.toString();
    }

    public static String uri(String issuer, String account, byte[] secret) {
        return "otpauth://totp/" + encode(issuer + ":" + account)
                + "?secret=" + base32(secret)
                + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1&digits=6&period=" + STEP_SECONDS;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
