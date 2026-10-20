package io.sala.krob_krong.account.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MfaStatusResponse {
    private Instant mfaEnabledAt;
}
