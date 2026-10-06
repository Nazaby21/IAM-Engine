package io.sala.krob_krong.account.service.impl;

import io.sala.krob_krong.account.dto.AuthResponse;
import io.sala.krob_krong.account.dto.LoginRequest;
import io.sala.krob_krong.account.dto.RegisterRequest;
import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.account.service.AuthService;
import io.sala.krob_krong.account.service.AuthSessionService;
import io.sala.krob_krong.account.specification.UserSpecification;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.iam.security.TokenService;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AuthSessionService sessions;
    private final TokenService tokens;
    private final String dummyPasswordHash;

    public AuthServiceImpl(
            UserRepository users, PasswordEncoder passwords, AuthSessionService sessions, TokenService tokens) {
        this.users = users;
        this.passwords = passwords;
        this.sessions = sessions;
        this.tokens = tokens;
        // Unknown accounts still incur password verification work.
        this.dummyPasswordHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Override
    public AuthResponse register(RegisterRequest request, String userAgent, String ip) {
        String email = normalizeEmail(request.getEmail());
        if (users.exists(UserSpecification.byEmail(email))) {
            throw new BusinessException(IAMErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setDisplayName(request.getDisplayName().strip());
        user.setPasswordHash(passwords.encode(request.getPassword()));
        // kind=person, active=true, unverified, no MFA or role grants: all server-owned defaults.
        return sessions.register(user, userAgent, ip);
    }

    @Override
    public AuthResponse login(LoginRequest request, String userAgent, String ip) {
        UserEntity user = users.findOne(UserSpecification.byEmail(normalizeEmail(request.getEmail())))
                .orElse(null);
        String hash = user != null && user.getPasswordHash() != null ? user.getPasswordHash() : dummyPasswordHash;
        boolean matches = passwords.matches(request.getPassword(), hash);
        if (!matches || user == null || user.getPasswordHash() == null || !user.isPerson()) {
            throw new BusinessException(IAMErrorCode.INVALID_CREDENTIALS);
        }
        return sessions.login(user.getId(), userAgent, ip);
    }

    @Override
    public AuthResponse refresh(String refreshToken, String userAgent, String ip) {
        // Identify the account without validating or revoking yet. The locked transaction re-reads the token.
        UUID userId = tokens.findRefreshToken(refreshToken).getUserId();
        return sessions.refresh(userId, refreshToken, userAgent, ip);
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
