package io.sala.krob_krong.account.controller;

import io.sala.krob_krong.account.dto.AuthResponse;
import io.sala.krob_krong.account.dto.LoginRequest;
import io.sala.krob_krong.account.dto.RefreshTokenRequest;
import io.sala.krob_krong.account.dto.RegisterRequest;
import io.sala.krob_krong.account.service.AuthService;
import io.sala.krob_krong.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        return ApiResponse.create(auth.register(request, http.getHeader(HttpHeaders.USER_AGENT), http.getRemoteAddr()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.success(auth.login(request, http.getHeader(HttpHeaders.USER_AGENT), http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        return ApiResponse.success(
                auth.refresh(request.getRefreshToken(), http.getHeader(HttpHeaders.USER_AGENT), http.getRemoteAddr()));
    }
}
