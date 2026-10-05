package io.sala.krob_krong.iam.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@FieldNameConstants
@Table(name = "refresh_token")
public class RefreshTokenEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private String familyId;

    @Column(name = "replaced_by")
    private String replacedBy;

    @Column(name = "user_agent")
    private String userAgent;

    private String ip;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
