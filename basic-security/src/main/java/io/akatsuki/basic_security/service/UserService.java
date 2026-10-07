package io.akatsuki.basic_security.service;

import io.akatsuki.basic_security.common.enums.GroupAge;
import io.akatsuki.basic_security.dto.request.UserRequest;
import io.akatsuki.basic_security.dto.response.UserResponse;

public interface UserService {

    UserResponse getId(String id);

    UserResponse createUser(UserRequest userRequest);

    Long countUserByGroup(GroupAge groupAge, String school);
}
