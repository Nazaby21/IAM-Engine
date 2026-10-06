package io.akatsuki.basic_security.service;

import io.akatsuki.basic_security.common.enums.AgeType;
import io.akatsuki.basic_security.dto.request.CreateUserRequestDto;
import io.akatsuki.basic_security.dto.response.CreateUserResponseDto;
import io.akatsuki.basic_security.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    CreateUserResponseDto createUser(CreateUserRequestDto request);

    UserResponse getId(String id);

    List<CreateUserResponseDto> getAllUsers();

    List<CreateUserResponseDto> getAgeQuery(AgeType age);

    List<CreateUserResponseDto> getUserByFilters(AgeType ageType,  String school);
}