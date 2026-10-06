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
@IdClass(RoleGrantRuleId.class)
@Table(name = "role_grant_rule")
public class RoleGrantRuleEntity {

    @Id
    @Column(name = "granter_role_id", nullable = false)
    private UUID granterRoleId;

    @Id
    @Column(name = "grantee_role_id", nullable = false)
    private UUID granteeRoleId;
}
