package io.sala.krob_krong.account.mfa;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "krob-krong.security.mfa")
public class MfaProperties {

    @NotBlank
    @Size(min = 32)
    private String encryptionKey;

    @NotBlank
    private String issuer = "Krob Krong";

    @Min(1)
    private int maxFailedAttempts = 5;

    @NotNull
    private Duration lockout = Duration.ofMinutes(15);
}
