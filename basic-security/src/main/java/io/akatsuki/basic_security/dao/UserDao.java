package io.akatsuki.basic_security.dao;

import io.akatsuki.basic_security.entity.UserEntity;
import java.util.Optional;

public interface UserDao {

    UserEntity save(UserEntity entity);

    Optional<UserEntity> findById(String id);
}
