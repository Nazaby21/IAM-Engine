package io.sala.krob_krong.enrollment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class EnrollmentRequest {

    @JsonProperty("user_id")
    @NotNull(message = "user id is required")
    private UUID userId;

    @JsonProperty("class_group_id")
    @NotNull(message = "class group id is required")
    private UUID classGroupId;
}
