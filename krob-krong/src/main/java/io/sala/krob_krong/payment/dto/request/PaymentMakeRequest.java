package io.sala.krob_krong.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.sala.krob_krong.payment.constant.Currency;
import io.sala.krob_krong.payment.constant.Method;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentMakeRequest {

    @JsonProperty("ticket_number")
    @NotBlank(message="ticket number is required")
    private String ticketNumber;

    @JsonProperty("amount")
    @NotNull(message = "amount is required")
    private BigDecimal amount;

    @JsonProperty("currency")
    @NotNull(message = "currency is required")
    private Currency currency;

    @JsonProperty("method")
    @NotNull(message = "payment method is required")
    private Method method;

    @JsonProperty("reference_image")
    @NotBlank(message = "please upload your reference image")
    private String referenceImage;
}
