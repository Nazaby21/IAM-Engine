package io.sala.krob_krong.payment.dto.response;

import io.sala.krob_krong.enrollment.dto.response.EnrollmentResponse;
import io.sala.krob_krong.payment.constant.Currency;
import io.sala.krob_krong.payment.constant.Method;
import io.sala.krob_krong.payment.constant.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class PaymentResponse {
    private String id;
    private EnrollmentResponse enrollment;
    private BigDecimal amount;
    private Currency currency;
    private Method method;
    private PaymentStatus status;
    private String referenceImage;
    private UUID createdBy;
    private Instant createdAt;
    private UUID updatedBy;
    private Instant updatedAt;
}
