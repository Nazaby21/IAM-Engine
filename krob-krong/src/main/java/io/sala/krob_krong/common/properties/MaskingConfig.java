package io.sala.krob_krong.common.properties;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@AllArgsConstructor
@ConfigurationProperties("mask")
public class MaskingConfig {

    public static final Set<String> DEFAULT_FIELDS = Set.of(
            "password",
            "cardnumber",
            "card_number",
            "cvv",
            "ssn",
            "token",
            "secret",
            "authorization",
            "apikey",
            "api_key",
            "accesstoken",
            "access_token",
            "refreshtoken",
            "refresh_token",
            "pin",
            "creditcard",
            "credit_card",
            "credential",
            "private_key");

    public static final List<Pattern> DEFAULT_PATTERNS =
            List.of(Pattern.compile("(?i).*(password|secret|token|key|ssn|card.?num|cvv|pin|credential).*"));

    public static final String DEFAULT_MASK = "****";

    public static final int DEFAULT_MAX_BODY_BYTES = 4096;

    private Set<String> sensitiveFields;
    private List<Pattern> sensitivePatterns;
    private String fullMask;
    private int partialUnmaskChars;
    private int maxBodyLogSize;

    /** Returns a default configuration covering the common sensitive fields. */
    public static MaskingConfig defaults() {
        return new MaskingConfig(DEFAULT_FIELDS, DEFAULT_PATTERNS, DEFAULT_MASK, 0, DEFAULT_MAX_BODY_BYTES);
    }

    /** True if the given field name should be masked. */
    public boolean isSensitive(String fieldName) {
        if (fieldName == null) return false;
        String lower = fieldName.toLowerCase();
        if (sensitiveFields != null && sensitiveFields.contains(lower)) return true;
        if (sensitivePatterns != null) {
            for (Pattern p : sensitivePatterns) {
                if (p.matcher(fieldName).matches()) return true;
            }
        }
        return false;
    }

    /** Masks a single value, honouring the {@link #partialUnmaskChars} window. */
    public String maskValue(String value) {
        if (value == null) return fullMask;
        if (partialUnmaskChars > 0 && value.length() > partialUnmaskChars) {
            return fullMask + value.substring(value.length() - partialUnmaskChars);
        }
        return fullMask;
    }
}
