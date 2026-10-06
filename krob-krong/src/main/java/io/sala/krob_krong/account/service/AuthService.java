package io.sala.krob_krong.account.service;

import io.sala.krob_krong.account.dto.AuthResponse;
import io.sala.krob_krong.account.dto.LoginRequest;
import io.sala.krob_krong.account.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request, String userAgent, String ip);

    AuthResponse login(LoginRequest request, String userAgent, String ip);

    AuthResponse refresh(String refreshToken, String userAgent, String ip);
}
