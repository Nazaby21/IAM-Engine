package io.akatsuki.basic_security.dao;

import io.akatsuki.basic_security.common.enums.GroupAge;
import io.akatsuki.basic_security.entity.UserEntity;
import java.util.Optional;

public interface UserDao {

    UserEntity save(UserEntity entity);

    Optional<UserEntity> findById(String id);

    boolean existsByEmail(String email);

    Long countUserByGroup(GroupAge group, String school);
}
