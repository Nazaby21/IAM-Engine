package io.sala.krob_krong.enrollment.service.validator;

import io.sala.krob_krong.classroom.entity.ClassGroupEntity;
import io.sala.krob_krong.classroom.repository.ClassGroupRepository;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.enrollment.constant.EnrollmentStatus;
import io.sala.krob_krong.enrollment.dto.request.EnrollmentRequest;
import io.sala.krob_krong.enrollment.entity.EnrollmentEntity;
import io.sala.krob_krong.enrollment.repository.EnrollmentRepository;
import io.sala.krob_krong.enrollment.service.TicketService;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EnrollmentValidator {

    private final EnrollmentRepository enrollmentRepository;
    private final TicketService ticketService;
    private final ClassGroupRepository classGroupRepository;

    public void validate(EnrollmentRequest request) {
        if (ObjectUtils.isEmpty(request)) {
            throw new BusinessException(IAMErrorCode.ENROLLMENT_REQUEST_INVALID);
        }
    }

    public EnrollmentEntity findById(String id) {
        return enrollmentRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.ENROLLMENT_NOT_FOUND));
    }

    public void isPending(EnrollmentEntity entity, IAMErrorCode errorCode) {
        if (!EnrollmentStatus.PENDING.name().equals(entity.getStatus())) {
            throw new BusinessException(errorCode);
        }
    }

    public void isTicketExist(String id) {
        if (ticketService.exists(id)) {
            throw new BusinessException(IAMErrorCode.TICKET_EXISTING);
        }
    }
    public void validateCapacity(UUID classGroupId) {
        ClassGroupEntity classGroup = classGroupRepository.findById(classGroupId)
                .orElseThrow(() ->
                        new BusinessException(IAMErrorCode.CLASS_GROUP_NOT_FOUND));

        long occupiedSeats = enrollmentRepository.countOccupiedSeats(
                classGroupId,
                List.of(
                        EnrollmentStatus.PAID.name(),
                        EnrollmentStatus.PROCESSING.name()
                )
        );

        if (occupiedSeats >= classGroup.getCapacity()) {
            throw new BusinessException(IAMErrorCode.CLASS_GROUP_FULL);
        }
    }
}
