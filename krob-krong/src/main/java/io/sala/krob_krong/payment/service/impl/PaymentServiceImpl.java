package io.sala.krob_krong.payment.service.impl;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.enrollment.constant.EnrollmentStatus;
import io.sala.krob_krong.enrollment.dto.response.EnrollmentResponse;
import io.sala.krob_krong.enrollment.entity.EnrollmentEntity;
import io.sala.krob_krong.enrollment.mapper.EnrollmentMapper;
import io.sala.krob_krong.enrollment.repository.EnrollmentRepository;
import io.sala.krob_krong.enrollment.service.TicketService;
import io.sala.krob_krong.enrollment.service.validator.EnrollmentValidator;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.payment.constant.PaymentStatus;
import io.sala.krob_krong.payment.dto.request.PaymentMakeRequest;
import io.sala.krob_krong.payment.dto.request.PaymentVerifyRequest;
import io.sala.krob_krong.payment.dto.response.PaymentResponse;
import io.sala.krob_krong.payment.entity.PaymentEntity;
import io.sala.krob_krong.payment.mapper.PaymentMapper;
import io.sala.krob_krong.payment.repository.PaymentRepository;
import io.sala.krob_krong.payment.service.PaymentService;
import io.sala.krob_krong.payment.service.validator.PaymentValidator;
import io.sala.krob_krong.school.service.SchoolAccess;
import io.sala.krob_krong.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentMapper paymentMapper;
    private final IdGenerator idGenerator;
    private final PaymentRepository paymentRepository;
    private final PaymentValidator paymentValidator;
    private final TicketService ticketService;
    private final EnrollmentMapper enrollmentMapper;
    private final EnrollmentValidator enrollmentValidator;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    public PaymentResponse makePayment(PaymentMakeRequest request) {
        paymentValidator.validate(request);
        String enrollmentId = ticketService.getEnrollmentId(request.getTicketNumber());
        PaymentEntity payment= paymentMapper.toEntity(request);
        UUID actor = SchoolAccess.actor();
        payment.setId(idGenerator.generatePaymentId());
        payment.setEnrollmentId(enrollmentId);
        payment.setStatus(PaymentStatus.PROCESSING.name());
        payment.setCreatedBy(actor);
        PaymentEntity save = paymentRepository.save(payment);
        PaymentResponse response = paymentMapper.toPayment(save);
        EnrollmentEntity enrollment = enrollmentValidator.findById(enrollmentId);
        response.setEnrollment(enrollmentMapper.toResponse(enrollment));
        EnrollmentResponse enrollmentResponse = enrollmentMapper.toResponse(enrollment);
        enrollmentResponse.setTicketNumber(ticketService.getTicketNumber(enrollmentId));
        response.setEnrollment(enrollmentResponse);
        return response;
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(PaymentStatus status, PaymentVerifyRequest request) {
        PaymentEntity payment = paymentValidator.validateVerification(status, request);
        payment.setStatus(status.name());
        EnrollmentEntity enrollment = enrollmentValidator.findById(payment.getEnrollmentId());
        UUID actor = SchoolAccess.actor();
        payment.setUpdatedBy(actor);
        enrollment.setUpdatedBy(actor);
        if (StringUtils.hasText(PaymentStatus.APPROVED.name())) {
            enrollment.setStatus(EnrollmentStatus.PAID.name());
        } else if (StringUtils.hasText(PaymentStatus.REFUNDED.name())) {
            enrollment.setStatus(EnrollmentStatus.CANCELLED.name());
        }
        enrollmentRepository.save(enrollment);
        PaymentEntity saved = paymentRepository.save(payment);
        PaymentResponse response = paymentMapper.toPayment(saved);
        EnrollmentResponse enrollmentResponse = enrollmentMapper.toResponse(enrollment);
        enrollmentResponse.setTicketNumber(ticketService.getTicketNumber(saved.getEnrollmentId()));
        response.setEnrollment(enrollmentResponse);
        return response;
    }

    @Override
    public PaymentResponse record(String paymentId) {

        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new BusinessException(IAMErrorCode.PAYMENT_NOT_FOUND));
        EnrollmentEntity enrollment = enrollmentValidator.findById(payment.getEnrollmentId());
        PaymentResponse response = paymentMapper.toPayment(payment);
        EnrollmentResponse enrollmentResponse = enrollmentMapper.toResponse(enrollment);
        enrollmentResponse.setTicketNumber(ticketService.getTicketNumber(payment.getEnrollmentId()));
        response.setEnrollment(enrollmentResponse);
        return response;
    }
}
