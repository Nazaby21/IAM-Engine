package io.sala.krob_krong.iam.config.security.impl;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.config.properties.JwtProperties;
import io.sala.krob_krong.iam.config.security.Principals;
import io.sala.krob_krong.iam.config.security.TokenService;
import io.sala.krob_krong.iam.dto.security.AccessToken;
import io.sala.krob_krong.iam.dto.security.IssuedRefresh;
import io.sala.krob_krong.iam.dto.security.SessionClaims;
import io.sala.krob_krong.iam.entity.RefreshTokenEntity;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.iam.repository.RefreshTokenRepository;
import io.sala.krob_krong.iam.specification.RefreshTokenSpecification;
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
import org.springframework.util.ObjectUtils;
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
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .subject(claims.getUserId())
                .claim("name", claims.getDisplayName())
                .claim(Principals.CLAIM_EMAIL, claims.getEmail())
                .claim(Principals.CLAIM_PLATFORM_ADMIN, Boolean.toString(claims.isPlatformAdmin()))
                .claim("permissions", List.copyOf(claims.getPermissionAuthorities()));

        if (StringUtils.hasText(claims.getTenantId())) {
            builder.claim(Principals.CLAIM_TENANT, claims.getTenantId());
        }
        if (!ObjectUtils.isEmpty(claims.getRolesKeys())) {
            builder.claim(Principals.CLAIM_ROLES, String.join(",", claims.getRolesKeys()));
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
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
    public IssuedRefresh issueRefreshToken(
            String userId, String tenantId, String familyId, String userAgent, String ip) {
        String raw = randomToken();
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(userId);
        entity.setTenantId(tenantId);
        entity.setTokenHash(hash(raw));
        entity.setFamilyId(
                StringUtils.hasText(familyId) ? familyId : UUID.randomUUID().toString());
        entity.setExpiresAt(Instant.now().plus(props.getRefreshTtl()));
        entity.setUserAgent(truncate(userAgent));
        entity.setIp(ip);
        refreshTokenRepository.save(entity);
        return IssuedRefresh.builder().rawValue(raw).entity(entity).build();
    }

    @Override
    public RefreshTokenEntity requireActive(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new BusinessException(IAMErrorCode.INVALID_REFRESH_TOKEN);
        }
        RefreshTokenEntity token = refreshTokenRepository
                .findOne(RefreshTokenSpecification.byTokenHash(hash(rawToken)))
                .orElseThrow(() -> new BusinessException(IAMErrorCode.INVALID_REFRESH_TOKEN));

        if (token.getRevokedAt() != null) {
            refreshTokenRepository.revokeFamily(token.getFamilyId());
            throw new BusinessException(IAMErrorCode.REFRESH_TOKEN_REUSE);
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(IAMErrorCode.INVALID_REFRESH_TOKEN);
        }
        return token;
    }

    @Override
    public void markRotated(RefreshTokenEntity previous, String replacedById) {
        previous.setRevokedAt(Instant.now());
        previous.setReplacedBy(replacedById);
        refreshTokenRepository.save(previous);
    }

    @Override
    public void revoke(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return;
        }
        refreshTokenRepository
                .findOne(RefreshTokenSpecification.byTokenHash(hash(rawToken)))
                .ifPresent(t -> refreshTokenRepository.revokeFamily(t.getFamilyId()));
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
