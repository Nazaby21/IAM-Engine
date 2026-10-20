package io.sala.krob_krong.classroom.entity;

import jakarta.persistence.*;

import java.sql.Timestamp;
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Timestamp deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;
}
