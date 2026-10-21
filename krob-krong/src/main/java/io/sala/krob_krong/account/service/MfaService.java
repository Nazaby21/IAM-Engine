package io.sala.krob_krong.account.service;

import io.sala.krob_krong.account.dto.MfaStatusResponse;
import io.sala.krob_krong.account.dto.TotpEnrollmentResponse;
import java.util.UUID;

public interface MfaService {
    TotpEnrollmentResponse startTotpEnrollment(UUID userId);

    MfaStatusResponse activateTotp(UUID userId, String code);

    void verifyLoginCode(UUID userId, String code);
}
