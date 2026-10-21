package io.sala.krob_krong.enrollment.dto.response;

import io.sala.krob_krong.enrollment.constant.EnrollmentStatus;
import lombok.Data;

@Data
public class EnrollmentResponse {
    private String id;
    private String ticketNumber;
    private String userId;
    private String classGroupId;
    private EnrollmentStatus Status;
    long expiresIn;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}
