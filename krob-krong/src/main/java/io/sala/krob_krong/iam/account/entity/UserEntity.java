package io.sala.krob_krong.iam.account.entity;

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
@Table(name = "app_user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String kind = "person";

    private String email;

    private String phone;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "avatar_asset_id")
    private UUID avatarAssetId;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "mfa_enabled_at")
    private Instant mfaEnabledAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isPerson() {
        return "person".equals(kind);
    }

    public boolean isService() {
        return "service".equals(kind);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }
}
