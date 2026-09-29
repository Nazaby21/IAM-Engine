package io.akatsuki.basic_security.service;

import io.akatsuki.basic_security.dto.response.UserResponse;

public interface UserService {

    UserResponse getId(String id);
}
