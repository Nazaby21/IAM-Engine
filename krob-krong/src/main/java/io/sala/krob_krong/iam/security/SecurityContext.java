package io.sala.krob_krong.iam.security;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record SecurityContext(
        String principalId,
        String principalName,
        String tokenId,
        Instant tokenIssuedAt,
        Instant tokenExpiresAt,
        Set<Permission> permissions,
        Map<String, String> claims,
        DeviceInfo deviceInfo,
        RiskScore riskScore,
        String sourceService,
        String requestId)
        implements Serializable {

    public boolean hasPermission(Permission required) {
        return permissions.stream().anyMatch(granted -> granted.implies(required));
    }

    public boolean hasPermission(String expression) {
        return hasPermission(Permission.parse(expression));
    }
}
