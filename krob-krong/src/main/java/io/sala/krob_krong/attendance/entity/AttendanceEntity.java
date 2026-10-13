package io.sala.krob_krong.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Entity
@FieldNameConstants
@Table(name = "attendances")
public class AttendanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "schedule_id")
    private String scheduleId;
    @Column(name = "enrollment_id")
    private String enrollmentId;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "date")
    private Date date;
    @Column(name = "status")
    private String status;
    @Column(name = "created_by")
    private UUID createdBy;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_by")
    private UUID updatedBy;
    @Column(name = "updated_at")
    private Instant updatedAt;
}
