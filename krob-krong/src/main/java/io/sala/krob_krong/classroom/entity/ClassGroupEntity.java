package io.sala.krob_krong.classroom.entity;

import io.sala.krob_krong.schedule.entity.ScheduleEntity;
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

    @Column(name = "classroom_id", length = 36)
    private UUID classroomId;

    @Column(name = "instructor_id")
    private UUID instructorId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer capacity = 0;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ScheduleEntity schedule;
}
