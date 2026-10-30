package io.sala.krob_krong.assessment.dto.request;

import io.sala.krob_krong.common.constant.AssessmentType;
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
public class CreateAssessmentRequest {

    private AssessmentType assessmentType;
    private String description;
    private String startDate;
    private String endDate;
    private Integer duration;
    private Integer totalScore;
}