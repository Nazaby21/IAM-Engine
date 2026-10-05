package io.sala.krob_krong.iam.config.security;

import java.io.Serializable;
import java.util.List;

public record RiskScore(double score, Level level, List<String> factors) implements Serializable {

    public enum Level {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public static RiskScore low() {
        return new RiskScore(0.0, Level.LOW, List.of());
    }

    public static RiskScore of(double score, List<String> factors) {
        Level level;
        if (score < 0.25) {
            level = Level.LOW;
        } else if (score < 0.50) {
            level = Level.MEDIUM;
        } else if (score < 0.75) {
            level = Level.HIGH;
        } else {
            level = Level.CRITICAL;
        }
        return new RiskScore(score, level, factors);
    }
}
