package io.sala.krob_krong.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Entity
@Getter
@Setter
@FieldNameConstants
@Table(name = "mfa_totp")
public class MfaTotpEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "secret_ciphertext", nullable = false)
    private String secretCiphertext;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "last_used_step")
    private Long lastUsedStep;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    public boolean isActive() {
        return activatedAt != null;
    }
}
