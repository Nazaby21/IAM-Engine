package io.sala.krob_krong.iam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Entity
@Getter
@Setter
@FieldNameConstants
@Table(name = "permission")
public class PermissionEntity {

    @Id
    private String code;

    @Column(nullable = false)
    private String description;

    @Column(name = "is_sensitive", nullable = false)
    private boolean sensitive = false;
}
