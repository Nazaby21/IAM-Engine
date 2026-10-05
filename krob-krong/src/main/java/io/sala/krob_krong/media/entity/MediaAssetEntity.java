package io.sala.krob_krong.media.entity;

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
@Table(name = "media_asset")
public class MediaAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private UUID uploadedBy;

    @Column(nullable = false, updatable = false)
    private String kind;

    @Column(nullable = false, updatable = false)
    private String purpose;

    @Column(nullable = false)
    private String visibility = "members";

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    private String sha256;

    @Column(name = "storage_key", nullable = false, unique = true, updatable = false)
    private String storageKey;

    @Column(nullable = false)
    private String status = "pending_upload";

    private Integer width;

    private Integer height;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isReady() {
        return "ready".equals(status);
    }
}
