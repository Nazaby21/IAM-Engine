package io.sala.krob_krong.enrollment.service;

import io.sala.krob_krong.enrollment.dto.request.EnrollmentCreateRequest;
import io.sala.krob_krong.enrollment.dto.response.EnrollmentResponse;

import java.util.UUID;

public interface EnrollmentService {
    EnrollmentResponse create (EnrollmentCreateRequest request);
    EnrollmentResponse exchange(UUID id);
}
