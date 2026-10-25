package io.sala.krob_krong.assessment.dto.response;

import io.sala.krob_krong.common.constant.AssessmentStatus;
import io.sala.krob_krong.common.constant.AssessmentType;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssessmentResponse {

    private UUID id;
    private UUID courseSubjectId;
    private AssessmentType type;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer duration;
    private Integer totalScore;
    private AssessmentStatus status;
}
