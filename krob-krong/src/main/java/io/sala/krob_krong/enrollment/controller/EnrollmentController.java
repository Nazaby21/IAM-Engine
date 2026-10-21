package io.sala.krob_krong.enrollment.controller;

import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.enrollment.dto.request.EnrollmentRequest;
import io.sala.krob_krong.enrollment.dto.response.EnrollmentResponse;
import io.sala.krob_krong.enrollment.dto.response.TicketResponse;
import io.sala.krob_krong.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    @PostMapping
    public ApiResponse<EnrollmentResponse> create(@Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.create(request);
        return ApiResponse.create(response);
    }

    @PostMapping("/{id}/update")
    public ApiResponse<EnrollmentResponse> update(
            @Valid @PathVariable String id, @RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.update(id, request);
        return ApiResponse.create(response);
    }

    @PostMapping("/{id}/exchange-ticket")
    public ApiResponse<TicketResponse> exchange(@PathVariable String id) {
        TicketResponse response = enrollmentService.exchange(id);
        return ApiResponse.success(response);
    }
}
