package io.sala.krob_krong.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MfaCodeRequest {
    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String code;
}
