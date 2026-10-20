package io.sala.krob_krong.enrollment.controller;

import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.enrollment.dto.request.EnrollmentCreateRequest;
import io.sala.krob_krong.enrollment.dto.response.EnrollmentResponse;
import io.sala.krob_krong.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    @PostMapping
    public ApiResponse<EnrollmentResponse> create (@Valid  @RequestBody EnrollmentCreateRequest request){
        EnrollmentResponse response = enrollmentService.create(request);
        return ApiResponse.create(response);
    }

    @PostMapping("/{id}/exchange-ticket")
    public ApiResponse<EnrollmentResponse> exchange (@PathVariable UUID id){
        EnrollmentResponse response = enrollmentService.exchange(id);
        return ApiResponse.success(response);
    }
}
