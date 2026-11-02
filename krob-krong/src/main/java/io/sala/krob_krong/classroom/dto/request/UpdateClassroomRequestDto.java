package io.sala.krob_krong.classroom.dto.request;

import io.sala.krob_krong.classroom.constants.ClassStatus;
import io.sala.krob_krong.classroom.constants.ClassType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UpdateClassroomRequestDto {

    @NotBlank(message = "School's name is required")
    private String schoolName;

    @NotBlank(message = "Room's name is required")
    private String name;

    private String floor;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 200, message = "Capacity must not exceed 500")
    private Integer capacity;

    @NotNull(message = "Class type is required")
    private ClassType classType;

    private ClassStatus status;
}
