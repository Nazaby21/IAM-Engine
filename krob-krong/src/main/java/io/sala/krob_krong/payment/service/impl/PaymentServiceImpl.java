package io.sala.krob_krong.payment.service.impl;

import io.sala.krob_krong.payment.constant.PaymentStatus;
import io.sala.krob_krong.payment.dto.request.PaymentMakeRequest;
import io.sala.krob_krong.payment.dto.response.PaymentResponse;
import io.sala.krob_krong.payment.entity.PaymentEntity;
import io.sala.krob_krong.payment.mapper.PaymentMapper;
import io.sala.krob_krong.payment.repository.PaymentRepository;
import io.sala.krob_krong.payment.service.PaymentService;
import io.sala.krob_krong.payment.service.validator.PaymentValidator;
import io.sala.krob_krong.school.service.SchoolAccess;
import io.sala.krob_krong.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentMapper paymentMapper;
    private final IdGenerator idGenerator;
    private final PaymentRepository paymentRepository;
    private final PaymentValidator paymentValidator;

    @Override
    public PaymentResponse makePayment(PaymentMakeRequest request) {
        PaymentEntity payment= paymentMapper.toEntity(request);
        UUID actor = SchoolAccess.actor();
        paymentValidator.validate(request);
        payment.setId(idGenerator.generatePaymentId());
        payment.setStatus(PaymentStatus.PROCESSING.name());
        payment.setCreatedBy(actor);
        PaymentEntity save = paymentRepository.save(payment);
        return  paymentMapper.toPayment(save);
    }
}
