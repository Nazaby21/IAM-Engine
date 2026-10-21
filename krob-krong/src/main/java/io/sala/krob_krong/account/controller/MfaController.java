package io.sala.krob_krong.account.controller;

import io.sala.krob_krong.account.dto.MfaCodeRequest;
import io.sala.krob_krong.account.dto.MfaStatusResponse;
import io.sala.krob_krong.account.dto.TotpEnrollmentResponse;
import io.sala.krob_krong.account.service.MfaService;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.iam.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/mfa")
public class MfaController {
    private final MfaService mfa;

    @PostMapping("/totp")
    public ApiResponse<TotpEnrollmentResponse> startTotpEnrollment(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.create(mfa.startTotpEnrollment(user.id()));
    }

    @PostMapping("/totp/activate")
    public ApiResponse<MfaStatusResponse> activateTotp(
            @Valid @RequestBody MfaCodeRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(mfa.activateTotp(user.id(), request.getCode()));
    }
}
