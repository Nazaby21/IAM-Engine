package io.sala.krob_krong.payment.service.validator;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.payment.dto.request.PaymentMakeRequest;
import io.sala.krob_krong.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentValidator {
    private final PaymentRepository paymentRepository;

    public void validate(PaymentMakeRequest request) {
        if (paymentRepository.existsByEnrollmentId(request.getEnrollmentId())) {
            throw new BusinessException(IAMErrorCode.PAYMENT_ALREADY_EXISTS);
        }
    }
}
