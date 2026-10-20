package io.sala.krob_krong.enrollment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EnrollmentCreateRequest {

    @JsonProperty("user_id")
    @NotNull(message = "user id is required")
    private UUID userId;

    @JsonProperty("class_group_id")
    @NotNull(message = "class group id is required")
    private UUID classGroupId;
}
