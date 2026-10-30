package io.sala.krob_krong.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class PaymentVerifyRequest {

    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("ticket_number")
    private String ticketNumber;

    @JsonProperty("secret_key")
    @NotBlank(message = "secret is required for verification")
    private String secretKey;

    @AssertTrue(message = "Provide either payment_id or ticket_number, not both")
    public boolean isValidIdentifier() {
        return StringUtils.hasText(paymentId) ^ StringUtils.hasText(ticketNumber);
    }
}