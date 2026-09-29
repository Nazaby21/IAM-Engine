package io.akatsuki.basic_security.service.impl;

import io.akatsuki.basic_security.dao.UserDao;
import io.akatsuki.basic_security.dto.response.UserResponse;
import io.akatsuki.basic_security.entity.UserEntity;
import io.akatsuki.basic_security.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    @Override
    public UserResponse getId(String id) {
        UserEntity user = userDao.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserResponse(user.getId());
    }
}
