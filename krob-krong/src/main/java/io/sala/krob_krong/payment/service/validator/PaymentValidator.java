package io.sala.krob_krong.payment.service.validator;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.enrollment.service.TicketService;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.payment.constant.PaymentStatus;
import io.sala.krob_krong.payment.dto.request.PaymentMakeRequest;
import io.sala.krob_krong.payment.dto.request.PaymentVerifyRequest;
import io.sala.krob_krong.payment.entity.PaymentEntity;
import io.sala.krob_krong.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class PaymentValidator {
    private final TicketService ticketService;
    private final PaymentRepository paymentRepository;

    @Value("${payment.verification.secret-key}")
    private String secretKey;

    public String validate(PaymentMakeRequest request) {
        String enrollmentId =
                ticketService.getEnrollmentId(request.getTicketNumber());
        boolean exists = paymentRepository
                .existsByEnrollmentIdAndStatus(
                        enrollmentId,
                        PaymentStatus.PROCESSING.name()
                );
        if (exists) {
            throw new BusinessException(
                    IAMErrorCode.PAYMENT_ALREADY_EXISTS
            );
        }
        return enrollmentId;
    }

    public PaymentEntity validateVerification(PaymentStatus status, PaymentVerifyRequest request){
        validateSecretKey(request.getSecretKey());
        validateVerificationStatus(status);
        PaymentEntity payment = findPayment(request);
        if (!PaymentStatus.PROCESSING.name().equals(payment.getStatus())) {
            throw new BusinessException(
                    IAMErrorCode.PAYMENT_ALREADY_VERIFIED
            );
        }
        return payment;
    }

    private void validateSecretKey(String requestSecret) {
        if (!StringUtils.hasText(requestSecret)
                || !MessageDigest.isEqual(
                secretKey.getBytes(StandardCharsets.UTF_8),
                requestSecret.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new BusinessException(
                    IAMErrorCode.INVALID_SECRET_KEY
            );
        }
    }

    private void validateVerificationStatus(PaymentStatus status) {
        if (status != PaymentStatus.APPROVED
                && status != PaymentStatus.REJECTED) {
            throw new BusinessException(
                    IAMErrorCode.INVALID_PAYMENT_STATUS
            );
        }
    }

    private PaymentEntity findPayment(PaymentVerifyRequest request) {
        if (StringUtils.hasText(request.getPaymentId())) {
            return paymentRepository.findById(request.getPaymentId())
                    .orElseThrow(() ->
                            new BusinessException(
                                    IAMErrorCode.PAYMENT_NOT_FOUND
                            ));
        }
        String enrollmentId =
                ticketService.getEnrollmentId(request.getTicketNumber());
        return paymentRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() ->
                        new BusinessException(
                                IAMErrorCode.PAYMENT_NOT_FOUND
                        ));
    }
}
