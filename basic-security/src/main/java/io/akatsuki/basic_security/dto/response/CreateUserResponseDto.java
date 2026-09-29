package io.akatsuki.basic_security.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CreateUserResponseDto {

    private String id;

    private String name;

    private String email;

    private Integer age;

    private String gender;
}