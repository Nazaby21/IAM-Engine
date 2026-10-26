package io.sala.krob_krong.schedule.service.impl;

import io.sala.krob_krong.attendance.entity.ScheduleEntity;
import io.sala.krob_krong.classroom.repository.ClassGroupRepository;
import io.sala.krob_krong.classroom.repository.ClassroomRepository;
import io.sala.krob_krong.common.enumz.DayOfWeeks;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.schedule.dto.request.CreateScheduleRequestDto;
import io.sala.krob_krong.schedule.dto.response.ScheduleResponseDto;
import io.sala.krob_krong.schedule.mapper.ScheduleMapper;
import io.sala.krob_krong.schedule.repository.ScheduleRepository;
import io.sala.krob_krong.schedule.service.ScheduleService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ClassGroupRepository classGroupRepository;
    private final ClassroomRepository classroomRepository;
    private final ScheduleMapper scheduleMapper;

    @Override
    @Transactional
    public ScheduleResponseDto createSchedule(CreateScheduleRequestDto requestDto) {

        UUID classGroupId = requestDto.getClassGroupId();

        classGroupRepository
                .findById(classGroupId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.CLASS_GROUP_NOT_FOUND));

        List<ScheduleEntity> entities = new ArrayList<>();

        for (var item : requestDto.getSchedule()) {
            classroomRepository
                    .findById(item.getClassroomId())
                    .orElseThrow(() -> new BusinessException(IAMErrorCode.CLASSROOM_NOT_FOUND));
            if (!item.getStartedTime().isBefore(item.getEndedTime())) {
                throw new IllegalArgumentException("Started time must be before ended time");
            }
            boolean conflict = scheduleRepository.existsScheduleConflict(
                    classGroupId,
                    item.getClassroomId(),
                    item.getDayOfWeek().name(),
                    item.getStartedTime(),
                    item.getEndedTime());

            if (conflict) {
                throw new BusinessException(IAMErrorCode.DUPLICATE_SCHEDULE);
            }

            ScheduleEntity entity = ScheduleEntity.builder()
                    .startedTime(item.getStartedTime())
                    .endedTime(item.getEndedTime())
                    .description(requestDto.getDescription())
                    .createdBy(UUID.fromString("f6ad7847-9f47-47b9-b7c4-5b3c26dd0255"))
                    .build();

            entities.add(entity);
        }

        List<ScheduleEntity> savedEntities = scheduleRepository.saveAll(entities);

        return ScheduleResponseDto.builder()
                .classGroupId(classGroupId)
                .description(requestDto.getDescription())
                .schedule(savedEntities.stream()
                        .map(entity -> ScheduleResponseDto.data
                                .builder()
                                .startedTime(entity.getStartedTime())
                                .endedTime(entity.getEndedTime())
                                .build())
                        .toList())
                .build();
    }
}
