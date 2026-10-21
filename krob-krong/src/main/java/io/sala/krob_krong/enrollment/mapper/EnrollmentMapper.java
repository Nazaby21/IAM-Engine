package io.sala.krob_krong.enrollment.mapper;

import io.sala.krob_krong.enrollment.dto.request.EnrollmentRequest;
import io.sala.krob_krong.enrollment.dto.response.EnrollmentResponse;
import io.sala.krob_krong.enrollment.dto.response.TicketResponse;
import io.sala.krob_krong.enrollment.entity.EnrollmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {
    EnrollmentResponse toResponse(EnrollmentEntity entity);

    EnrollmentEntity toEntity(EnrollmentRequest request);

    @Mapping(target = "enrollmentId", source = "id")
    TicketResponse toTicket(EnrollmentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "classGroupId", source = "classGroupId")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(EnrollmentRequest request, @MappingTarget EnrollmentEntity entity);
}
