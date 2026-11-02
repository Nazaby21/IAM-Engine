package io.sala.krob_krong.course_subject.dto.response;

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
public class ClassGroupResponse {
    private UUID id;
    private String name;
    private Integer capacity;
    private UUID classroomId;
    private InstructorResponse instructor;
    private String status;
}
