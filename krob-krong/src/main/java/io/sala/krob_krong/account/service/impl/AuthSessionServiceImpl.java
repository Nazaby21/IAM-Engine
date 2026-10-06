package io.sala.krob_krong.account.service.impl;

import io.sala.krob_krong.account.dto.AuthResponse;
import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.account.service.AuthSessionService;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.iam.security.TokenService;
import io.sala.krob_krong.iam.security.dto.IssuedRefresh;
import io.sala.krob_krong.iam.security.dto.SessionClaims;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthSessionServiceImpl implements AuthSessionService {
    private final UserRepository users;
    private final TokenService tokens;

    @Override
    @Transactional
    public AuthResponse register(UserEntity user, String userAgent, String ip) {
        users.saveAndFlush(user);
        return issue(user, tokens.issueRefreshToken(user.getId(), null, userAgent, ip));
    }

    @Override
    @Transactional
    public AuthResponse login(UUID userId, String userAgent, String ip) {
        UserEntity user = lockUser(userId);
        requireNormalUser(user);
        return issue(user, tokens.issueRefreshToken(userId, null, userAgent, ip));
    }

    @Override
    @Transactional
    public AuthResponse refresh(UUID userId, String raw, String userAgent, String ip) {
        // All refreshes for this user serialize before reading token state or inserting a successor.
        // Reuse revocation can commit independently: no refresh-token rows have been changed or locked yet.
        UserEntity user = lockUser(userId);
        var previous = tokens.requireActive(raw);
        if (!userId.equals(previous.getUserId())) {
            throw new BusinessException(IAMErrorCode.INVALID_REFRESH_TOKEN);
        }
        requireNormalUser(user);
        var next = tokens.issueRefreshToken(userId, previous.getFamilyId(), userAgent, ip);
        AuthResponse response = issue(user, next);
        tokens.markRotated(previous, next.getEntity().getId());
        return response;
    }

    private UserEntity lockUser(UUID userId) {
        return users.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.INVALID_CREDENTIALS));
    }

    private static void requireNormalUser(UserEntity user) {
        if (!user.isPerson()) {
            throw new BusinessException(IAMErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new BusinessException(IAMErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getMfaEnabledAt() != null) {
            throw new BusinessException(IAMErrorCode.MFA_REQUIRED);
        }
    }

    private AuthResponse issue(UserEntity user, IssuedRefresh refresh) {
        var access = tokens.issueAccessToken(SessionClaims.builder()
                .userId(user.getId())
                .sessionId(refresh.getEntity().getFamilyId())
                .kind("person")
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .mfaVerified(false)
                .build());
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .tokenType("Bearer")
                .accessToken(access.getValue())
                .expiresIn(tokens.accessTtlSeconds())
                .accessTokenExpiresAt(access.getExpiresAt())
                .refreshToken(refresh.getRawValue())
                .refreshTokenExpiresAt(refresh.getEntity().getExpiresAt())
                .build();
    }
}
