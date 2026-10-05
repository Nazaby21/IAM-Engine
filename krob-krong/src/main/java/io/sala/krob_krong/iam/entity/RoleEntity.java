package io.sala.krob_krong.iam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@FieldNameConstants
@Table(name = "role")
public class RoleEntity {

    @Id
    private String id;

    /** {@code null} for global system templates. */
    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "role_key", nullable = false)
    private String roleKey;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private int rank;

    @Column(name = "parent_role_id")
    private String parentRoleId;

    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    @Column(name = "is_default", nullable = false)
    private boolean defaultRole = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_code"))
    private Set<PermissionEntity> permissions = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
