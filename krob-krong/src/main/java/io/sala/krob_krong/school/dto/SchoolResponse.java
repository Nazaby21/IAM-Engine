package io.sala.krob_krong.school.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolResponse {
    private UUID id;
    private UUID createdBy;
    private String displayName;
    private String legalName;
    private String registrationNo;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private UUID logoAssetId;
    private UUID coverAssetId;
    private String logoUrl;
    private String coverUrl;
    private String status;
    private String statusReason;
    private Instant statusChangedAt;
    private Instant createdAt;
}
