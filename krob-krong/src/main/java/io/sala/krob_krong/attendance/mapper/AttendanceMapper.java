package io.sala.krob_krong.attendance.mapper;

import io.sala.krob_krong.attendance.dto.response.AttendanceResponseDto;
import io.sala.krob_krong.attendance.entity.AttendanceEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AttendanceMapper {

    List<AttendanceResponseDto> toResponses(List<AttendanceEntity> attendanceEntity);

    AttendanceResponseDto toResponse(AttendanceEntity attendanceEntity);
}
