package io.sala.krob_krong.iam.security.impl;

import io.sala.krob_krong.account.entity.RefreshTokenEntity;
import io.sala.krob_krong.account.repository.RefreshTokenRepository;
import io.sala.krob_krong.account.specification.RefreshTokenSpecification;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.iam.security.TokenService;
import io.sala.krob_krong.iam.security.config.JwtProperties;
import io.sala.krob_krong.iam.security.dto.AccessToken;
import io.sala.krob_krong.iam.security.dto.IssuedRefresh;
import io.sala.krob_krong.iam.security.dto.SessionClaims;
import io.sala.krob_krong.iam.security.jwt.TokenClaims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;
    private final RefreshTokenRepository refreshTokenRepository;

    // ---- Access tokens ------------------------------------------------------
    @Override
    public AccessToken issueAccessToken(SessionClaims claims) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(props.getTtl());

        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(props.getIssuer())
                .audience(List.of(props.getAudience()))
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .subject(claims.getUserId().toString())
                .claim(TokenClaims.SESSION_ID, claims.getSessionId().toString())
                .claim(TokenClaims.KIND, claims.getKind())
                .claim(TokenClaims.MFA, claims.isMfaVerified());
        if (StringUtils.hasText(claims.getDisplayName())) {
            builder.claim(TokenClaims.NAME, claims.getDisplayName());
        }
        if (StringUtils.hasText(claims.getEmail())) {
            builder.claim(TokenClaims.EMAIL, claims.getEmail());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type(TokenClaims.ACCESS_TOKEN_TYPE)
                .build();
        String value = jwtEncoder
                .encode(JwtEncoderParameters.from(header, builder.build()))
                .getTokenValue();
        return AccessToken.builder().value(value).expiresAt(expiresAt).build();
    }

    @Override
    public long accessTtlSeconds() {
        return props.getTtl().toSeconds();
    }

    // ---- Refresh tokens -----------------------------------------------------
    @Override
    public IssuedRefresh issueRefreshToken(UUID userId, UUID sessionId, String userAgent, String ip) {
        String raw = randomToken();
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(userId);
        entity.setFamilyId(sessionId != null ? sessionId : UUID.randomUUID());
        entity.setTokenHash(hash(raw));
        entity.setExpiresAt(Instant.now().plus(props.getRefreshTtl()));
        entity.setUserAgent(truncate(userAgent));
        entity.setIp(ip);
        refreshTokenRepository.save(entity);
        return IssuedRefresh.builder().rawValue(raw).entity(entity).build();
    }

    @Override
    public RefreshTokenEntity findRefreshToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new BusinessException(IAMErrorCode.INVALID_REFRESH_TOKEN);
        }
        return refreshTokenRepository
                .findOne(RefreshTokenSpecification.byTokenHash(hash(rawToken)))
                .orElseThrow(() -> new BusinessException(IAMErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Override
    public RefreshTokenEntity requireActive(String rawToken) {
        RefreshTokenEntity token = findRefreshToken(rawToken);
        if (token.getRevokedAt() != null) {
            refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now());
            throw new BusinessException(IAMErrorCode.REFRESH_TOKEN_REUSE);
        }
        if (!token.getExpiresAt().isAfter(Instant.now())) {
            throw new BusinessException(IAMErrorCode.INVALID_REFRESH_TOKEN);
        }
        return token;
    }

    @Override
    public void markRotated(RefreshTokenEntity previous, UUID replacedById) {
        if (refreshTokenRepository.rotate(previous.getId(), replacedById, Instant.now()) == 0) {
            refreshTokenRepository.revokeFamily(previous.getFamilyId(), Instant.now());
            throw new BusinessException(IAMErrorCode.REFRESH_TOKEN_REUSE);
        }
    }

    @Override
    public void revoke(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return;
        }
        refreshTokenRepository
                .findOne(RefreshTokenSpecification.byTokenHash(hash(rawToken)))
                .ifPresent(t -> refreshTokenRepository.revokeFamily(t.getFamilyId(), Instant.now()));
    }

    // ---- helpers ------------------------------------------------------------

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** SHA-256 hex of the raw token — only this is persisted, never the token itself. */
    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
