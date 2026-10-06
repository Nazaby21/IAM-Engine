package io.sala.krob_krong.account.service;

import io.sala.krob_krong.account.dto.AuthResponse;
import io.sala.krob_krong.account.entity.UserEntity;
import java.util.UUID;

/** Database-only session operations; password hashing and verification happen before these transactions. */
public interface AuthSessionService {
    AuthResponse register(UserEntity user, String userAgent, String ip);

    AuthResponse login(UUID userId, String userAgent, String ip);

    AuthResponse refresh(UUID userId, String refreshToken, String userAgent, String ip);
}
