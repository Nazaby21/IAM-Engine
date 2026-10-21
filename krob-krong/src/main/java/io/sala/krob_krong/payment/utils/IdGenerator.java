package io.sala.krob_krong.payment.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {

    public String generatePaymentId() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        String uuid =
                UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        return "PAY-" + date + "-" + uuid;
    }
}
