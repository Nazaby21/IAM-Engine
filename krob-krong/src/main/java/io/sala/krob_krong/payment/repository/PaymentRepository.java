package io.sala.krob_krong.payment.repository;

import io.sala.krob_krong.payment.entity.PaymentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    boolean existsByEnrollmentId(String id);
}
