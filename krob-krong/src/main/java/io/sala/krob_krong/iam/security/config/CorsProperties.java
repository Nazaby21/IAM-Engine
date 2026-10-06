package io.sala.krob_krong.iam.security.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "krob-krong.security.cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();

    private Duration maxAge = Duration.ofHours(1);
}
