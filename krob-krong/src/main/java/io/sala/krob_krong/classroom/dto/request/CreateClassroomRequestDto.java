package io.sala.krob_krong.classroom.dto.request;

import io.sala.krob_krong.classroom.constants.ClassStatus;
import io.sala.krob_krong.classroom.constants.ClassType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @Size(max = 200, message = "Capacity must not greater than 200")
    private Integer capacity;

    @NotNull(message = "Class type is required")
    private ClassType classType;

}
