package io.sala.krob_krong.iam.config.security;

import io.sala.krob_krong.iam.dto.security.AccessToken;
import io.sala.krob_krong.iam.dto.security.IssuedRefresh;
import io.sala.krob_krong.iam.dto.security.SessionClaims;
import io.sala.krob_krong.iam.entity.RefreshTokenEntity;

public interface TokenService {

    AccessToken issueAccessToken(SessionClaims claims);

    long accessTtlSeconds();

    IssuedRefresh issueRefreshToken(String userId, String tenantId, String familyId, String userAgent, String ip);

    RefreshTokenEntity requireActive(String rawToken);

    void markRotated(RefreshTokenEntity previous, String replacedById);

    void revoke(String rawToken);
}
