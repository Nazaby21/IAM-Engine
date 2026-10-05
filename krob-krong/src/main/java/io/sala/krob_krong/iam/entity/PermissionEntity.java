package io.sala.krob_krong.iam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * A single capability in the catalog, natural-keyed as {@code module:action} (e.g. {@code roles:update}).
 * Issued into JWTs as the framework permission {@code module:action:*}.
 */
@Entity
@Getter
@Setter
@FieldNameConstants
@Table(name = "permission")
public class PermissionEntity {

    /** {@code module:action}. */
    @Id
    private String code;

    @Column(name = "module_code", nullable = false)
    private String moduleCode;

    @Column(nullable = false)
    private String action;

    private String description;

    /** The framework permission string enforced by {@code @Authorize}: {@code module:action:*}. */
    public String toAuthority() {
        return code + ":*";
    }
}
