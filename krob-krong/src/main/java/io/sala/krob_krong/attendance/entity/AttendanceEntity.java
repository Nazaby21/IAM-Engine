package io.sala.krob_krong.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@FieldNameConstants
@Table(name = "attendance")
public class AttendanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(name = "enrollment_id")
    private String enrollmentId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "attendance_date")
    private LocalDate attendance_date;

    @Column(name = "status")
    private String status;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @CreationTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
