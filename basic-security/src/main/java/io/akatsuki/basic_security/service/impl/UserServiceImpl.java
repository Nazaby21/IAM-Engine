package io.akatsuki.basic_security.service.impl;

import io.akatsuki.basic_security.common.enums.GroupAge;
import io.akatsuki.basic_security.dao.UserDao;
import io.akatsuki.basic_security.dto.request.UserRequest;
import io.akatsuki.basic_security.dto.response.UserResponse;
import io.akatsuki.basic_security.entity.UserEntity;
import io.akatsuki.basic_security.repository.UserRepository;
import io.akatsuki.basic_security.service.UserService;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final UserRepository userRepository;

    @Override
    public UserResponse getId(String id) {
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserResponse(
                user.getId(), user.getName(), user.getGender(), user.getEmail(), user.getAge(), user.getSchool());
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.email())) {
            throw new ValidationException("Email already exists");
        }

        UserEntity user = new UserEntity();
        user.setName(userRequest.name());
        user.setEmail(userRequest.email());
        user.setPassword(userRequest.password());
        user.setGender(userRequest.gender());
        user.setAge(userRequest.age());
        user.setSchool(userRequest.school());

        UserEntity saved = userRepository.save(user);
        return new UserResponse(
                saved.getId(), saved.getName(), saved.getEmail(), saved.getGender(), saved.getAge(), saved.getSchool());
    }

    @Override
    public Long countUserByGroup(GroupAge groupAge, String school) {
        return userRepository.countUserByGroup(groupAge, school);
    }
}
