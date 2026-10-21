package io.sala.krob_krong.course.mapper;

import io.sala.krob_krong.course.dto.CourseCreateRequest;
import io.sala.krob_krong.course.dto.CourseResponse;
import io.sala.krob_krong.course.dto.CourseUpdateRequest;
import io.sala.krob_krong.course.entity.CourseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CourseMapper {
    CourseResponse toResponse(CourseEntity courseEntity);

    CourseEntity toEntity(CourseCreateRequest request);

    void updateEntity(CourseUpdateRequest request, @MappingTarget CourseEntity course);
}
