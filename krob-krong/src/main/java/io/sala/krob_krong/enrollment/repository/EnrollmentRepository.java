package io.sala.krob_krong.enrollment.repository;

import io.sala.krob_krong.enrollment.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, String> {}
