package io.sala.krob_krong.course_subject.entity;

import io.sala.krob_krong.subject.entity.SubjectEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("subjectId")
    @JoinColumn(name = "subject_id")
    private SubjectEntity subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "course_id", referencedColumnName = "course_id", insertable = false, updatable = false),
            @JoinColumn(name = "subject_id", referencedColumnName = "subject_id", insertable = false, updatable = false)
    })
    private CourseSubjectEntity courseSubject;
}