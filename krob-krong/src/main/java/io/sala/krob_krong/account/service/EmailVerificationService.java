package io.sala.krob_krong.account.service;

import io.sala.krob_krong.account.dto.VerificationResponse;

public interface EmailVerificationService {
    VerificationResponse request();

    VerificationResponse confirm(String token);
}
