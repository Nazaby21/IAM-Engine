package io.sala.krob_krong.assessment.entity;

import io.sala.krob_krong.common.constant.AssessmentStatus;
import io.sala.krob_krong.common.constant.AssessmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@FieldNameConstants
@Table(name = "assessment")
public class AssessmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private AssessmentType type;

    @Column(name = "description")
    private String description;

    @Column(name = "started_date")
    private Instant startedDate;

    @Column(name = "ended_date")
    private Instant endedDate;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "total_score")
    private BigDecimal totalScore;

    @Column(name = "percentage")
    private BigDecimal percentage;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AssessmentStatus status;

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
