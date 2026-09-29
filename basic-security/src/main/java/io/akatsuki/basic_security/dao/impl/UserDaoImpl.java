package io.akatsuki.basic_security.dao.impl;

import io.acmwchsd.data.query.Query;
import io.acmwchsd.data.repository.ACMRepository;
import io.akatsuki.basic_security.dao.UserDao;
import io.akatsuki.basic_security.entity.UserEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserDaoImpl implements UserDao {

    private final ACMRepository repository;

    @Override
    public UserEntity save(UserEntity entity) {
        // logic validation before insert to databse
        return repository.save(entity);
    }

    @Override
    public Optional<UserEntity> findById(String id) {
        Query<UserEntity> query = Query.from(UserEntity.class)
                .where(u -> u.get(UserEntity.Fields.id).eq(id));
        return repository.one(query);
    }
}
