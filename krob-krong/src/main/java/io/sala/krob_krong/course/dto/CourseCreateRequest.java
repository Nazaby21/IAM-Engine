package io.sala.krob_krong.course.dto;

import io.sala.krob_krong.common.enumz.CourseType;
import jakarta.validation.constraints.NotNull;
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
public class CourseCreateRequest {
    @NotNull(message = "Course name is require")
    private String name;

    @NotNull(message = "Course type is require")
    private CourseType type;

    private String description;

    @NotNull(message = "Course status is require")
    private String status;
}
