package io.akatsuki.basic_security.dao;

import io.akatsuki.basic_security.common.enums.AgeType;
import io.akatsuki.basic_security.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface UserDao {

    UserEntity save(UserEntity entity);

    Optional<UserEntity> findById(String id);

    List<UserEntity> findAllUsers();

    List<UserEntity> findByAgeType(AgeType ageType);

    List<UserEntity> findByAgeTypeOrSchool(AgeType ageType, String school);

}