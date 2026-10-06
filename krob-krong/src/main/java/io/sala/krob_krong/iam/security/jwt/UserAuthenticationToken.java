package io.sala.krob_krong.iam.security.jwt;

import io.sala.krob_krong.iam.security.AuthenticatedUser;
import java.io.Serial;
import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;

public final class UserAuthenticationToken extends AbstractOAuth2TokenAuthenticationToken<Jwt> {

    @Serial
    private static final long serialVersionUID = 1L;

    public UserAuthenticationToken(
            Jwt jwt, AuthenticatedUser user, Collection<? extends GrantedAuthority> authorities) {
        super(jwt, user, jwt, authorities);
        setAuthenticated(true);
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return (AuthenticatedUser) super.getPrincipal();
    }

    @Override
    public Map<String, Object> getTokenAttributes() {
        return getToken().getClaims();
    }
}
