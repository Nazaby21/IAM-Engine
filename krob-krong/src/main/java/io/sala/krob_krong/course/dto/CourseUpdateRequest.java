package io.sala.krob_krong.course.dto;

import io.sala.krob_krong.common.enumz.CourseType;
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
public class CourseUpdateRequest {
    private String name;
    private CourseType type;
    private String description;
    private String status;
}
