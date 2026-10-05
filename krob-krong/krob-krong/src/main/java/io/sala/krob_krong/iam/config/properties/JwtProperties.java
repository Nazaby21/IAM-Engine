package io.sala.krob_krong.iam.config.properties;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "krob-krong.security.jwt")
public class JwtProperties {
    private String secret = "change-me-in-prod-please-32bytes-minimum-secret-key";
    private String issuer = "krob-krong";
    private Duration ttl = Duration.ofHours(12);
    private Duration refreshTtl = Duration.ofDays(30);
}
