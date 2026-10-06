package io.sala.krob_krong.iam.security.config;

import io.sala.krob_krong.iam.security.AuthenticatedUser;
import io.sala.krob_krong.iam.security.jwt.ActiveSessionValidator;
import io.sala.krob_krong.iam.security.jwt.TokenClaims;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtTypeValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties props) {
        return NimbusJwtEncoder.withSecretKey(signingKey(props))
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties props, ActiveSessionValidator activeSessionValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(signingKey(props))
                .macAlgorithm(MacAlgorithm.HS256)
                // Nimbus only accepts typ "JWT"; JwtTypeValidator below enforces "at+jwt" instead.
                .validateType(false)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(props.getClockSkew()),
                new JwtClaimValidator<>(JwtClaimNames.EXP, Objects::nonNull),
                new JwtIssuerValidator(props.getIssuer()),
                new JwtAudienceValidator(props.getAudience()),
                new JwtTypeValidator(TokenClaims.ACCESS_TOKEN_TYPE),
                new JwtClaimValidator<>(JwtClaimNames.SUB, JwtConfig::isUuid),
                new JwtClaimValidator<>(TokenClaims.SESSION_ID, JwtConfig::isUuid),
                new JwtClaimValidator<String>(
                        TokenClaims.KIND,
                        kind -> AuthenticatedUser.KIND_PERSON.equals(kind)
                                || AuthenticatedUser.KIND_SERVICE.equals(kind)),
                activeSessionValidator));
        return decoder;
    }

    private static SecretKey signingKey(JwtProperties props) {
        return new SecretKeySpec(props.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static boolean isUuid(String value) {
        if (value == null) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException _) {
            return false;
        }
    }
}
