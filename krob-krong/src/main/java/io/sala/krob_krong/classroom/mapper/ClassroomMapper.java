package io.sala.krob_krong.classroom.mapper;

import io.sala.krob_krong.classroom.dto.request.CreateClassroomRequestDto;
import io.sala.krob_krong.classroom.dto.request.UpdateClassroomRequestDto;
import io.sala.krob_krong.classroom.dto.response.ClassroomResponseDto;
import io.sala.krob_krong.classroom.entity.ClassroomEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClassroomMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "schoolId", ignore = true)
    @Mapping(target = "branchId", ignore = true)
    @Mapping(target = "number", ignore = true)
    @Mapping(target = "status", constant = "AVAILABLE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ClassroomEntity toEntity(CreateClassroomRequestDto requestDto);

    @Mapping(target = "schoolName", source = "schoolName")
    ClassroomResponseDto toResponse(ClassroomEntity classroom, String schoolName);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "schoolId", ignore = true)
    @Mapping(target = "branchId", ignore = true)
    @Mapping(target = "number", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(UpdateClassroomRequestDto requestDto, @MappingTarget ClassroomEntity classroom);
}
