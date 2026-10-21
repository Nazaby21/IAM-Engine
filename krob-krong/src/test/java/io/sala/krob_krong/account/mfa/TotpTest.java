package io.sala.krob_krong.account.mfa;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TotpTest {

    private static final byte[] RFC_SECRET = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    @ParameterizedTest
    @CsvSource({"59, 287082", "1111111109, 081804", "1111111111, 050471", "1234567890, 005924", "2000000000, 279037"})
    void matchesRfc6238Sha1Vectors(long epochSecond, String expected) {
        assertThat(Totp.code(RFC_SECRET, Totp.step(Instant.ofEpochSecond(epochSecond))))
                .isEqualTo(expected);
    }

    @Test
    void acceptsOneStepOfClockDriftEitherWay() {
        Instant now = Instant.ofEpochSecond(1_234_567_890);
        long step = Totp.step(now);

        assertThat(Totp.match(RFC_SECRET, Totp.code(RFC_SECRET, step - 1), now, null))
                .hasValue(step - 1);
        assertThat(Totp.match(RFC_SECRET, Totp.code(RFC_SECRET, step + 1), now, null))
                .hasValue(step + 1);
        assertThat(Totp.match(RFC_SECRET, Totp.code(RFC_SECRET, step + 2), now, null))
                .isEmpty();
    }

    @Test
    void rejectsACodeFromAnAlreadyUsedStep() {
        Instant now = Instant.ofEpochSecond(1_234_567_890);
        long step = Totp.step(now);
        String code = Totp.code(RFC_SECRET, step);

        assertThat(Totp.match(RFC_SECRET, code, now, step)).isEmpty();
        assertThat(Totp.match(RFC_SECRET, code, now, step - 1)).hasValue(step);
    }

    @Test
    void encodesBase32WithoutPadding() {
        assertThat(Totp.base32("foobar".getBytes(StandardCharsets.US_ASCII))).isEqualTo("MZXW6YTBOI");
        assertThat(Totp.base32(RFC_SECRET)).isEqualTo("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
    }

    @Test
    void buildsAnOtpauthUriForAuthenticatorApps() {
        assertThat(Totp.uri("Krob Krong", "admin@example.com", RFC_SECRET))
                .isEqualTo("otpauth://totp/Krob%20Krong%3Aadmin%40example.com"
                        + "?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=Krob%20Krong"
                        + "&algorithm=SHA1&digits=6&period=30");
    }
}
