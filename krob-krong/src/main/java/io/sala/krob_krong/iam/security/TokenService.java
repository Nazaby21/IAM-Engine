package io.sala.krob_krong.iam.security;

import io.sala.krob_krong.iam.security.dto.AccessToken;
import io.sala.krob_krong.iam.security.dto.IssuedRefresh;
import io.sala.krob_krong.iam.security.dto.SessionClaims;
import io.sala.krob_krong.iam.account.entity.RefreshTokenEntity;

public interface TokenService {

    AccessToken issueAccessToken(SessionClaims claims);

    long accessTtlSeconds();

    IssuedRefresh issueRefreshToken(String userId, String tenantId, String familyId, String userAgent, String ip);

    RefreshTokenEntity requireActive(String rawToken);

    void markRotated(RefreshTokenEntity previous, String replacedById);

    void revoke(String rawToken);
}
