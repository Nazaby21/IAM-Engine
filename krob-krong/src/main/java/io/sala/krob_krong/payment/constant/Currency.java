package io.sala.krob_krong.payment.constant;

import java.math.BigDecimal;

public enum Currency {
    KHR(new BigDecimal(5000)),
    USD(new BigDecimal(5)),
    YUAN(new BigDecimal(100000)),
    DONG(new BigDecimal(5));

    private final BigDecimal minAmount;

    Currency(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(minAmount) >= 0;
    }
}
