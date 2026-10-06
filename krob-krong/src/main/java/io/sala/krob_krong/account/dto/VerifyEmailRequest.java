package io.sala.krob_krong.account.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailRequest {
    @NotBlank
    @Pattern(regexp = "[0-9a-f]{64}")
    private String token;
}
