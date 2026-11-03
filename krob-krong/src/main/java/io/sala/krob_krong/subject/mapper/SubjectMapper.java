package io.sala.krob_krong.subject.mapper;

import io.sala.krob_krong.subject.dto.SubjectCreateRequest;
import io.sala.krob_krong.subject.dto.SubjectResponse;
import io.sala.krob_krong.subject.dto.SubjectUpdateRequest;
import io.sala.krob_krong.subject.entity.SubjectEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SubjectMapper {
    SubjectResponse toResponse(SubjectEntity subject);

    SubjectEntity toEntity(SubjectCreateRequest request);

    void updateEntity(SubjectUpdateRequest request, @MappingTarget SubjectEntity entity);
}
