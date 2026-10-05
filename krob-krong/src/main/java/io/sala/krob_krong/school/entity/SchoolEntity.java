package io.sala.krob_krong.school.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "school")
public class SchoolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "registration_no")
    private String registrationNo;

    private String description;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "logo_asset_id")
    private UUID logoAssetId;

    @Column(name = "cover_asset_id")
    private UUID coverAssetId;

    @Column(nullable = false)
    private String status = "draft";

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
