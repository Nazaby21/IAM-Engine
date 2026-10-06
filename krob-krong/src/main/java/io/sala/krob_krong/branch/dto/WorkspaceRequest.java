package io.sala.krob_krong.branch.dto;

import jakarta.validation.constraints.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceRequest {
    @NotBlank
    @Pattern(regexp = "[a-z0-9][a-z0-9-]{1,61}[a-z0-9]")
    private String slug;
}
