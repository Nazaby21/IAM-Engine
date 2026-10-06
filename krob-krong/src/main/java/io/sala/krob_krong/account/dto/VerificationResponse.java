package io.sala.krob_krong.account.dto;

import java.time.Instant;
import lombok.*;

@Getter
@AllArgsConstructor
public class VerificationResponse {
    private boolean verified;
    private Instant expiresAt;
}
