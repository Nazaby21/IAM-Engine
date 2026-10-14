package io.sala.krob_krong.classroom.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Entity
@Getter
@Setter
@FieldNameConstants
@Table(name = "class_group")
public class ClassGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36)
    private UUID id;

    @Column(name = "course_id", length = 36, nullable = false)
    private UUID courseId;

    @Column(name = "subject_id", length = 36, nullable = false)
    private UUID subjectId;

    @Column(name = "classroom_id", length = 36)
    private UUID classroomId;

    @Column(name = "instructor_id")
    private UUID instructorId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "capacity")
    private int capacity;

    @Column(name = "status", length = 30, nullable = false)
    private String status;
}
