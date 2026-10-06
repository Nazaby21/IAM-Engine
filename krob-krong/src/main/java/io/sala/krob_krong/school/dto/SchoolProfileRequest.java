package io.sala.krob_krong.school.dto;

import jakarta.validation.constraints.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolProfileRequest {
    @NotBlank
    @Size(min = 2, max = 120)
    private String displayName;

    @Size(max = 200)
    private String legalName;

    @Size(max = 100)
    private String registrationNo;

    @Size(max = 5000)
    private String description;

    @Email
    @Size(max = 254)
    private String contactEmail;

    @Size(max = 40)
    private String contactPhone;
}
