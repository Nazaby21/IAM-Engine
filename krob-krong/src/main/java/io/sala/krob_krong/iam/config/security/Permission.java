package io.sala.krob_krong.iam.config.security;

import java.io.Serializable;
import lombok.NonNull;

public record Permission(String resource, String action, String scope) implements Serializable {

    private static final String WILDCARD = "*";
    private static final String DELIMITER = ":";

    public static Permission parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Permission expression must not be blank");
        }
        String[] parts = expression.split(DELIMITER, 3);
        return switch (parts.length) {
            case 1 -> new Permission(parts[0], WILDCARD, WILDCARD);
            case 2 -> new Permission(parts[0], parts[1], WILDCARD);
            default -> new Permission(parts[0], parts[1], parts[2]);
        };
    }

    public boolean implies(Permission required) {
        return matches(this.resource, required.resource)
                && matches(this.action, required.action)
                && matches(this.scope, required.scope);
    }

    private static boolean matches(String granted, String required) {
        return WILDCARD.equals(granted) || granted.equals(required);
    }

    @Override
    @NonNull
    public String toString() {
        return resource + DELIMITER + action + DELIMITER + scope;
    }
}
