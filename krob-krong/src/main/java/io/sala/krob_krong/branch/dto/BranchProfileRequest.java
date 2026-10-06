package io.sala.krob_krong.branch.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.*;

@Getter
@Setter
public class BranchProfileRequest {
    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Size(max = 500)
    private String addressLine;

    @Size(max = 100)
    private String district;

    @NotBlank
    @Size(max = 100)
    private String province;

    @NotBlank
    @Pattern(regexp = "[A-Z]{2}")
    private String countryCode = "KH";

    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    @Digits(integer = 3, fraction = 6)
    private BigDecimal latitude;

    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    @Digits(integer = 3, fraction = 6)
    private BigDecimal longitude;

    @NotBlank
    @Size(max = 100)
    private String timezone = "Asia/Phnom_Penh";
}
