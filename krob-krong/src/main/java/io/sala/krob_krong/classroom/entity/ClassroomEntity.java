package io.sala.krob_krong.classroom.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Entity
@Getter
@Setter
@FieldNameConstants
@Table("classroom")
public class ClassroomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "number", length = 50)
    private String number;

    @Column(name = "floor")
    private String floor;

    @Column(name = "capacity")
    private String capacity;

    @Column(name = "class_type", length = 30)
    private String classType;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
