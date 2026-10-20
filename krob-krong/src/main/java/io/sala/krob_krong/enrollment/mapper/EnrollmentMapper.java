package io.sala.krob_krong.enrollment.mapper;

import io.sala.krob_krong.enrollment.dto.request.EnrollmentCreateRequest;
import io.sala.krob_krong.enrollment.dto.response.EnrollmentResponse;
import io.sala.krob_krong.enrollment.entity.EnrollmentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {
    EnrollmentResponse toResponse (EnrollmentEntity entity);
    EnrollmentEntity toEntity (EnrollmentCreateRequest request);
}
