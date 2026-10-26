package io.sala.krob_krong.enrollment.service;

import io.sala.krob_krong.enrollment.dto.request.EnrollmentRequest;
import io.sala.krob_krong.enrollment.dto.response.EnrollmentResponse;
import io.sala.krob_krong.enrollment.dto.response.TicketResponse;

public interface EnrollmentService {
    EnrollmentResponse create(EnrollmentRequest request);

    EnrollmentResponse update(String id, EnrollmentRequest request);

    TicketResponse exchange(String id);
}
