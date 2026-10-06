package io.sala.krob_krong.iam.security;

import io.sala.krob_krong.account.entity.RefreshTokenEntity;
import io.sala.krob_krong.iam.security.dto.AccessToken;
import io.sala.krob_krong.iam.security.dto.IssuedRefresh;
import io.sala.krob_krong.iam.security.dto.SessionClaims;
import java.util.UUID;

public interface TokenService {

    AccessToken issueAccessToken(SessionClaims claims);

    long accessTtlSeconds();

    IssuedRefresh issueRefreshToken(UUID userId, UUID sessionId, String userAgent, String ip);

    /** Lookup only; callers must validate again after acquiring the account lock before rotating. */
    RefreshTokenEntity findRefreshToken(String rawToken);

    RefreshTokenEntity requireActive(String rawToken);

    void markRotated(RefreshTokenEntity previous, UUID replacedById);

    void revoke(String rawToken);
}
