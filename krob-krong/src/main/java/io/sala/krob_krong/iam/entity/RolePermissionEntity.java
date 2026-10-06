package io.sala.krob_krong.iam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Entity
@Getter
@Setter
@FieldNameConstants
@IdClass(RolePermissionId.class)
@Table(name = "role_permission")
public class RolePermissionEntity {

    @Id
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Id
    @Column(name = "permission_code", nullable = false)
    private String permissionCode;

    @Id
    @Column(nullable = false, columnDefinition = "access_scope")
    private String scope;
}
