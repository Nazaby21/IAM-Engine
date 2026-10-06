package io.sala.krob_krong.audit.entity;

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

@Entity
@Getter
@Setter
@FieldNameConstants
@Table(name = "access_log")
public class AccessLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant at;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "permission_code", nullable = false)
    private String permissionCode;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(nullable = false)
    private boolean allowed;
}
