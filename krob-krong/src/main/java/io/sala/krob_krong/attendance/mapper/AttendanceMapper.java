package io.sala.krob_krong.attendance.mapper;

import io.sala.krob_krong.attendance.dto.request.AttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.response.AttendanceResponseDto;
import io.sala.krob_krong.attendance.entity.AttendanceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AttendanceMapper {

    //request to entity
    @Mapping(source = "checkInDate", target = "date")
    AttendanceEntity toAttendanceEntity(AttendanceRequestDto request);

    //entity to response
    @Mapping(source = "date", target = "checkInDate")
    List<AttendanceResponseDto> toAttendanceResponse(AttendanceEntity entity);
}
