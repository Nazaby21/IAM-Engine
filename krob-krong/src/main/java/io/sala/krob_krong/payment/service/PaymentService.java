package io.sala.krob_krong.payment.service;

import io.sala.krob_krong.payment.dto.request.PaymentMakeRequest;
import io.sala.krob_krong.payment.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse makePayment(PaymentMakeRequest request);
}
