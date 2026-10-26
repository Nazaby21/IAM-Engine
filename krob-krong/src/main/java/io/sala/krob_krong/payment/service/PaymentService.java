package io.sala.krob_krong.payment.service;

import io.sala.krob_krong.payment.constant.PaymentStatus;
import io.sala.krob_krong.payment.dto.request.PaymentMakeRequest;
import io.sala.krob_krong.payment.dto.request.PaymentVerifyRequest;
import io.sala.krob_krong.payment.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse makePayment(PaymentMakeRequest request);

    PaymentResponse verifyPayment (PaymentStatus status, PaymentVerifyRequest request);

    PaymentResponse record (String paymentId);
}
