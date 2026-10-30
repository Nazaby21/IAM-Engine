package io.sala.krob_krong.announcement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "announcement")
public class AnnouncementEntity {
    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "class_group_id")
    private UUID classGroupId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "type")
    private String type;

    @Column(name = "attachment")
    private String attachment;

    @Column(name = "status")
    private String status;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "published_at")
    private Instant publishedAt;

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
