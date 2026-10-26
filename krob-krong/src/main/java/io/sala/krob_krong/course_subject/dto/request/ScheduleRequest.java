package io.sala.krob_krong.course_subject.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
public class ScheduleRequest {
    private UUID id;

    @NotBlank(message = "Schedule name is required")
    @Size(max = 100, message = "Schedule name must not exceed 100 characters")
    private String name;

    @NotEmpty(message = "At least one day is required")
    private Set<DayOfWeek> daysOfWeek;

    @NotNull(message = "Start time is required")
    private LocalTime startedTime;

    @NotNull(message = "End time is required")
    private LocalTime endedTime;

    @NotNull(message = "Max students is required")
    @Min(value = 1, message = "Max students must be at least 1")
    private Integer maxStudents;

    @Size(max = 255)
    private String description;

    @Size(max = 30)
    private String status;

    @Valid
    private List<ClassGroupRequest> classGroups;
}
