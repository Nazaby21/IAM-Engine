package io.sala.krob_krong.schedule.mapper;

import io.sala.krob_krong.common.enumz.DayOfWeeks;
import io.sala.krob_krong.schedule.dto.request.CreateScheduleRequestDto;
import io.sala.krob_krong.schedule.dto.response.ScheduleResponseDto;
import io.sala.krob_krong.schedule.entity.ScheduleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ScheduleMapper {

    @Mapping(
            target = "dayOfWeek",
            source = "dayOfWeek",
            qualifiedByName = "mapDaysToString"
    )
    ScheduleEntity toEntity(CreateScheduleRequestDto requestDto);

    @Mapping(
            target = "dayOfWeek",
            source = "dayOfWeek",
            qualifiedByName = "mapDaysToString"
    )
    ScheduleResponseDto toResponseDto(ScheduleEntity scheduleEntity);

    @Named("mapDaysToString")
    default String mapDaysToString(List<DayOfWeeks> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }

        return days.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    @Named("mapDaysToString")
    default List<DayOfWeeks> mapDaysToString(String days) {
        if (days == null || days.isBlank()) {
            return List.of();
        }

        return Arrays.stream(days.split(","))
                .map(String::trim)
                .map(DayOfWeeks::valueOf)
                .toList();
    }
}
