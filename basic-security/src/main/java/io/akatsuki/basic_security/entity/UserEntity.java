package io.akatsuki.basic_security.entity;

import io.acmwchsd.data.annotation.ACMSequence;
import io.acmwchsd.data.id.IdStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
@Entity(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id")
    @ACMSequence(prefix = "USR", strategy = IdStrategy.SNOWFLAKE)
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender")
    private String gender;
}