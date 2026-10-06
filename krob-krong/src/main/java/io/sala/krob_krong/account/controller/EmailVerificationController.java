package io.sala.krob_krong.account.controller;

import io.sala.krob_krong.account.dto.*;
import io.sala.krob_krong.account.service.EmailVerificationService;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EmailVerificationController {
    private final EmailVerificationService verification;

    @PostMapping("/me/email-verification")
    public ApiResponse<VerificationResponse> request() {
        return ApiResponse.success(verification.request());
    }

    @PostMapping(value = "/auth/verify-email", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<VerificationResponse> confirm(@Valid @RequestBody VerifyEmailRequest request) {
        return ApiResponse.success(verification.confirm(request.getToken()));
    }

    // GET is deliberately non-consuming: email scanners may follow links automatically.
    @GetMapping(value = "/auth/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public String confirmationPage(@RequestParam String token) {
        validateToken(token);
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\"><title>Verify your email</title></head>"
                + "<body><main><h1>Verify your email</h1><p>Confirm your email address to register a school on Krob Krong.</p>"
                + "<form method=\"post\" action=\"verify-email\"><input type=\"hidden\" name=\"token\" value=\"" + token
                + "\">"
                + "<button type=\"submit\">Verify email</button></form></main></body></html>";
    }

    @PostMapping(
            value = "/auth/verify-email",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String confirmFromBrowser(@RequestParam String token) {
        validateToken(token);
        verification.confirm(token);
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Email verified</title></head>"
                + "<body><main><h1>Email verified</h1><p>You can now return to Krob Krong and register your school.</p></main></body></html>";
    }

    private static void validateToken(String token) {
        if (token == null || !token.matches("[0-9a-f]{64}"))
            throw new BusinessException(IAMErrorCode.INVALID_VERIFICATION_TOKEN);
    }
}
