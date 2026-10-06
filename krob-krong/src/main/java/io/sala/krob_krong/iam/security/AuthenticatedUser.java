package io.sala.krob_krong.iam.security;

import java.io.Serializable;
import java.util.UUID;
import org.springframework.security.core.AuthenticatedPrincipal;

public record AuthenticatedUser(
        UUID id, UUID sessionId, String kind, String email, String displayName, boolean mfaVerified)
        implements AuthenticatedPrincipal, Serializable {

    public static final String KIND_PERSON = "person";
    public static final String KIND_SERVICE = "service";

    @Override
    public String getName() {
        return id.toString();
    }

    public boolean isPerson() {
        return KIND_PERSON.equals(kind);
    }
}
