package io.sala.krob_krong.course_subject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseSubjectCompensationId implements Serializable {
    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "subject_id")
    private UUID subjectId;
}
