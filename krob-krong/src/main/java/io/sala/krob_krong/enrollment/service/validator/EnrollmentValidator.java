package io.sala.krob_krong.enrollment.service.validator;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.enrollment.dto.request.EnrollmentCreateRequest;
import io.sala.krob_krong.enrollment.entity.EnrollmentEntity;
import io.sala.krob_krong.enrollment.repository.EnrollmentRepository;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EnrollmentValidator {

    private final EnrollmentRepository enrollmentRepository;

    public void validate(EnrollmentCreateRequest request) {
        if (ObjectUtils.isEmpty(request)) {
            throw new BusinessException(
                    IAMErrorCode.ENROLLMENT_REQUEST_INVALID
            );
        }
    }

    public EnrollmentEntity findById(UUID id){
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        IAMErrorCode.ENROLLMENT_NOT_FOUND
                ));
    }
}
