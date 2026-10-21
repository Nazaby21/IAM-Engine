package io.sala.krob_krong.subject.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
public class SubjectCreateRequest {
    @NotNull
    private String name;

    private BigDecimal credit;

    @NotNull
    private String status;
}
