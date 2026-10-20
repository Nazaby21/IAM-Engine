package io.sala.krob_krong.classroom.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.sala.krob_krong.classroom.constants.ClassStatus;
import io.sala.krob_krong.classroom.constants.ClassType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonPropertyOrder({
        "id",
        "name",
        "classType",
        "status",
        "floor",
        "capacity",
        "schoolId",
        "schoolName",
        "branchId"
})
public class ClassroomResponseDto {

    // identity
    private UUID id;

    // classroom details
    private String name;
    private ClassType classType;
    private ClassStatus status;
    private String floor;
    private Integer capacity;

    // ownership
    private UUID schoolId;
    private String schoolName;
    private UUID branchId;
}