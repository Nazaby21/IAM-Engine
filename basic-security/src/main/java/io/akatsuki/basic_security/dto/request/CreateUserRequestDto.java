package io.akatsuki.basic_security.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequestDto {

    private String id;

    private String name;

    private String email;

    private String password;

    private Integer age;

    private String gender;
}