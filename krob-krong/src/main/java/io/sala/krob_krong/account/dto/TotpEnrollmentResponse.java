package io.sala.krob_krong.account.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TotpEnrollmentResponse {
    private String secret;
    private String otpauthUri;
}
