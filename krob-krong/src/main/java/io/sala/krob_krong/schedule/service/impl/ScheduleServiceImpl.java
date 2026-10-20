package io.sala.krob_krong.schedule.service.impl;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.schedule.dto.request.CreateScheduleRequestDto;
import io.sala.krob_krong.schedule.dto.response.ScheduleResponseDto;
import io.sala.krob_krong.schedule.entity.ScheduleEntity;
import io.sala.krob_krong.schedule.mapper.ScheduleMapper;
import io.sala.krob_krong.schedule.repository.ScheduleRepository;
import io.sala.krob_krong.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;

    @Override
    public ScheduleResponseDto createSchedule(CreateScheduleRequestDto createScheduleRequestDto) {
        log.debug("createSchedule: createScheduleRequestDto={}", createScheduleRequestDto);
        if (!scheduleRepository.existsByClassroomIdAndClassGroupId(createScheduleRequestDto.getClassroomId(), createScheduleRequestDto.getClassGroupId())) {
            throw  new BusinessException(IAMErrorCode.CLASSROOM_AND_CLASS_GROUP_NOT_FOUND);
        }
        ScheduleEntity entity = scheduleMapper.toEntity(createScheduleRequestDto);

        entity.setCreatedBy(UUID.fromString("f6ad7847-9f47-47b9-b7c4-5b3c26dd0255"));

        return scheduleMapper.toResponseDto(scheduleRepository.save(entity));
    }
}
