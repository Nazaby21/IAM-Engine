package io.sala.krob_krong.course_subject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Entity
@Getter
@Setter
@FieldNameConstants
@Table(name = "course_subject")
public class CourseSubjectEntity {
    @EmbeddedId
    private CourseSubjectCompensationId id;

    @Column(name = "instructor_id")
    private UUID instructorId;

    @Column(name = "code")
    private String code;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "price_type")
    private String priceType;

    @Column(name = "description")
    private String description;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "currency")
    private String currency;

    @Column(name = "status")
    private String status;

    @Column(name = "is_public")
    private Boolean isPublic;
}
