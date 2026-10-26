package io.sala.krob_krong.course_subject.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSubjectUpdateRequest {

    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    @DecimalMin(value = "0.00", message = "Price must not be negative")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 digits and 2 decimals")
    private BigDecimal price;

    @Size(max = 30, message = "Price type must not exceed 30 characters")
    private String priceType;

    private String description;
    private Instant startedAt;
    private Instant endedAt;

    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;

    @Size(max = 30, message = "Status must not exceed 30 characters")
    private String status;

    private Boolean isPublic;

    @AssertTrue(message = "End date must be on or after the start date")
    @Schema(hidden = true)
    public boolean isDateRangeValid() {
        return startedAt == null || endedAt == null || !endedAt.isBefore(startedAt);
    }

    private List<@Valid ScheduleRequest> schedules;
}
