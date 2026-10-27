package io.sala.krob_krong.enrollment.service.validator;

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

@Component
@RequiredArgsConstructor
public class EnrollmentValidator {

    private final EnrollmentRepository enrollmentRepository;
    private final TicketService ticketService;

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

    public void isPending(EnrollmentEntity entity) {
        if (!EnrollmentStatus.PENDING.name().equals(entity.getStatus())) {
            throw new BusinessException(IAMErrorCode.CANNOT_EXCHANGE);
        }
    }

    public void isTicketExist(String id) {
        if (ticketService.exists(id)) {
            throw new BusinessException(IAMErrorCode.TICKET_EXISTING);
        }
    }
}
