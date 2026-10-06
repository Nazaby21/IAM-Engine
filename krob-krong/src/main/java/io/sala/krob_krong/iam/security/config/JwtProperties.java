package io.sala.krob_krong.iam.security.config;

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
@ConfigurationProperties(prefix = "krob-krong.security.jwt")
public class JwtProperties {

    // No default: a signing key committed to the repo would let anyone mint tokens. HS256 needs >= 256 bits.
    @NotBlank
    @Size(min = 32)
    private String secret;

    @NotBlank
    private String issuer = "krob-krong";

    @NotBlank
    private String audience = "krob-krong-api";

    @NotNull
    private Duration ttl = Duration.ofMinutes(15);

    @NotNull
    private Duration refreshTtl = Duration.ofDays(30);

    @NotNull
    private Duration clockSkew = Duration.ofSeconds(30);
}
