package io.sala.krob_krong.course_subject.mapper;

import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.classroom.entity.ClassGroupEntity;
import io.sala.krob_krong.course_subject.dto.request.CourseSubjectCreateRequest;
import io.sala.krob_krong.course_subject.dto.request.CourseSubjectUpdateRequest;
import io.sala.krob_krong.course_subject.dto.response.ClassGroupResponse;
import io.sala.krob_krong.course_subject.dto.response.CourseSubjectResponse;
import io.sala.krob_krong.course_subject.dto.response.InstructorResponse;
import io.sala.krob_krong.course_subject.dto.response.ScheduleResponse;
import io.sala.krob_krong.course_subject.entity.CourseSubjectEntity;
import io.sala.krob_krong.schedule.entity.ScheduleEntity;
import io.sala.krob_krong.subject.mapper.SubjectMapper;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        uses = SubjectMapper.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CourseSubjectMapper {

    @Mapping(source = "id.courseId", target = "courseId")
    @Mapping(target = "schedules", ignore = true)
    CourseSubjectResponse toResponse(CourseSubjectEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "status", ignore = true)
    CourseSubjectEntity toEntity(CourseSubjectCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subject", ignore = true)
    void updateEntity(CourseSubjectUpdateRequest request, @MappingTarget CourseSubjectEntity entity);

    @Mapping(target = "daysOfWeek", source = "daysOfWeek", qualifiedByName = "sortedDays")
    ScheduleResponse toScheduleResponse(ScheduleEntity entity, @Context Map<UUID, UserEntity> users);

    List<ScheduleResponse> toScheduleResponses(List<ScheduleEntity> entities, @Context Map<UUID, UserEntity> users);

    @Mapping(target = "instructor", source = "instructorId", qualifiedByName = "instructorById")
    ClassGroupResponse toGroupResponse(ClassGroupEntity entity, @Context Map<UUID, UserEntity> users);

    List<ClassGroupResponse> toGroupResponses(List<ClassGroupEntity> groups, @Context Map<UUID, UserEntity> users);

    // Monday -> Sunday (enum order)
    @Named("sortedDays")
    default List<DayOfWeek> sortedDays(Set<DayOfWeek> days) {
        return days == null ? List.of() : days.stream().sorted().toList();
    }

    @Named("instructorById")
    default InstructorResponse instructorById(UUID id, @Context Map<UUID, UserEntity> users) {
        return id == null ? null : toInstructor(users.get(id));
    }

    default InstructorResponse toInstructor(UserEntity u) {
        if (u == null) return null;
        return InstructorResponse.builder()
                .userId(u.getId())
                .displayName(Stream.of(u.getFirstName(), u.getLastName())
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(" ")))
                .build();
    }
}
