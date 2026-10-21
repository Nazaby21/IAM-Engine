package io.sala.krob_krong.enrollment.utils;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {
    public String enroll() {
        return "ENROLL-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
