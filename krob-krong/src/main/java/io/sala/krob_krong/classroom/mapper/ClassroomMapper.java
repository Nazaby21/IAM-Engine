package io.sala.krob_krong.classroom.mapper;

import io.sala.krob_krong.classroom.dto.request.CreateClassroomRequestDto;
import io.sala.krob_krong.classroom.dto.request.UpdateClassroomRequestDto;
import io.sala.krob_krong.classroom.dto.response.ClassroomResponseDto;
import io.sala.krob_krong.classroom.entity.ClassroomEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ClassroomMapper {

    @Mapping(target = "status", constant = "AVAILABLE")
    ClassroomEntity toEntity(CreateClassroomRequestDto requestDto);

    @Mapping(target = "schoolName", source = "schoolName")
    ClassroomResponseDto toResponse(ClassroomEntity classroom, String schoolName);

    void updateEntity(UpdateClassroomRequestDto requestDto, @MappingTarget ClassroomEntity classroom);
}
