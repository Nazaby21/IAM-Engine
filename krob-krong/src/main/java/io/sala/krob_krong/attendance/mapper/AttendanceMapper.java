package io.sala.krob_krong.attendance.mapper;

import io.sala.krob_krong.attendance.dto.response.AttendanceResponseDto;
import io.sala.krob_krong.attendance.entity.AttendanceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AttendanceMapper {

    // request to entity
    //    @Mapping(source = "attendanceDate", target = "attendanceDate")
    //    AttendanceEntity toAttendanceEntity(AttendanceRequestDto request);

    AttendanceResponseDto toAttendanceResponse(AttendanceEntity entity);
}
