package io.sala.krob_krong.payment.controller;

import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.payment.constant.PaymentStatus;
import io.sala.krob_krong.payment.dto.request.PaymentMakeRequest;
import io.sala.krob_krong.payment.dto.request.PaymentVerifyRequest;
import io.sala.krob_krong.payment.dto.response.PaymentResponse;
import io.sala.krob_krong.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ApiResponse<PaymentResponse> makePayment(@Valid @RequestBody PaymentMakeRequest request) {
        PaymentResponse response = paymentService.makePayment(request);
        return ApiResponse.create(response);
    }

    @PostMapping ("/verify")
    public ApiResponse<PaymentResponse> verifyPayment (@RequestParam PaymentStatus status, @Valid @RequestBody PaymentVerifyRequest request){
        PaymentResponse response=paymentService.verifyPayment(status, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}/record")
    public ApiResponse <PaymentResponse> record (@PathVariable String id){
        PaymentResponse response=paymentService.record(id);
        return ApiResponse.success(response);
    }
}
