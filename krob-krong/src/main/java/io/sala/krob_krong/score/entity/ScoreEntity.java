package io.sala.krob_krong.score.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@FieldNameConstants
@Table(name = "score")
public class ScoreEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "assessment_id")
    private UUID assessmentId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "score")
    private Integer score;

    @Column(name = "grade")
    private String grade;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}