package io.sala.krob_krong.course_subject.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassGroupRequest {
    private UUID id;

    @NotBlank(message = "Group name is required")
    @Size(max = 100, message = "Group name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private UUID classroomId;

    private UUID instructorId;

    @Size(max = 30)
    private String status;
}