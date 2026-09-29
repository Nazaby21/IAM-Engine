package io.akatsuki.basic_security.dao;

import io.akatsuki.basic_security.common.enums.AgeType;
import io.akatsuki.basic_security.entity.UserEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

public interface UserDao {

    UserEntity save(UserEntity entity);

    Optional<UserEntity> findById(String id);

    List<UserEntity> findAllUsers();

    List<UserEntity> findByAgeType(@PathVariable AgeType ageType);
}