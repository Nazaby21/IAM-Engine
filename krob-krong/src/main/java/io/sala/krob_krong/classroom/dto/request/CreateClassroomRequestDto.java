package io.sala.krob_krong.classroom.dto.request;

import io.sala.krob_krong.classroom.constants.ClassType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClassroomRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    private String floor;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be positive number")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 200, message = "Capacity cannot exceed 50")
    private Integer capacity;

    @NotNull(message = "Class type is required")
    private ClassType classType;
}
