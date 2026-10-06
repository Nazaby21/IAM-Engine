package io.akatsuki.basic_security.service.impl;

import io.acmwchsd.core.exception.ACMException;
import io.akatsuki.basic_security.common.enums.AgeType;
import io.akatsuki.basic_security.common.error.AuthErrorCode;
import io.akatsuki.basic_security.dao.UserDao;
import io.akatsuki.basic_security.dto.request.CreateUserRequestDto;
import io.akatsuki.basic_security.dto.response.CreateUserResponseDto;
import io.akatsuki.basic_security.dto.response.UserResponse;
import io.akatsuki.basic_security.entity.UserEntity;
import io.akatsuki.basic_security.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    @Override
    public CreateUserResponseDto createUser(
            CreateUserRequestDto request) {

        UserEntity user = UserEntity.builder()
                .id(request.getId())
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .age(request.getAge())
                .gender(request.getGender())
                .build();

        UserEntity savedUser = userDao.save(user);

        return CreateUserResponseDto.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .age(savedUser.getAge())
                .gender(savedUser.getGender())
                .build();
    }

    @Override
    public UserResponse getId(String id) {
        UserEntity user = userDao.findById(id).orElseThrow(() -> new ACMException(AuthErrorCode.INVALID_CREDENTIAL));
        return new UserResponse(user.getId());
    }

    @Override
    public List<CreateUserResponseDto> getAllUsers() {

        List<UserEntity> users = userDao.findAllUsers();

        return users.stream()
                .map(user -> CreateUserResponseDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .age(user.getAge())
                        .gender(user.getGender())
                        .build())
                .toList();
    }

    @Override
    public List<CreateUserResponseDto> getAgeQuery(AgeType age) {
        List<UserEntity> users = userDao.findByAgeType(age);

        return users.stream()
                .map(user -> CreateUserResponseDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .age(user.getAge())
                        .gender(user.getGender())
                        .build())
                .toList();
    }

    @Override
    public List<CreateUserResponseDto> getUserByFilters(AgeType ageType, String school) {
        List<UserEntity> users = userDao.findByAgeTypeOrSchool(ageType, school);

        return users.stream()
                .map(user -> CreateUserResponseDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .age(user.getAge())
                        .gender(user.getGender())
                        .build())
                .toList();
    }
}