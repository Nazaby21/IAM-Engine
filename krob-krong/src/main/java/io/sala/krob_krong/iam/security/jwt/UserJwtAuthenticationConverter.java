package io.sala.krob_krong.iam.security.jwt;

import io.sala.krob_krong.iam.security.AuthenticatedUser;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

// Identity only: permissions are per-school and must reflect revocations immediately, so RbacAuthorizer evaluates them.
@Component
public class UserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaimAsString(TokenClaims.SESSION_ID)),
                jwt.getClaimAsString(TokenClaims.KIND),
                jwt.getClaimAsString(TokenClaims.EMAIL),
                jwt.getClaimAsString(TokenClaims.NAME),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean(TokenClaims.MFA)));
        var kindAuthority = new SimpleGrantedAuthority("ROLE_" + user.kind().toUpperCase(Locale.ROOT));
        return new UserAuthenticationToken(jwt, user, List.of(kindAuthority));
    }
}
