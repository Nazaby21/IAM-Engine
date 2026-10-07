package io.akatsuki.basic_security.dao.impl;

import static io.akatsuki.basic_security.common.enums.GroupAge.Young;

import io.acmwchsd.data.query.Query;
import io.acmwchsd.data.repository.ACMRepository;
import io.akatsuki.basic_security.common.enums.GroupAge;
import io.akatsuki.basic_security.dao.UserDao;
import io.akatsuki.basic_security.entity.UserEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Slf4j
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
                .where(u -> u.get(UserEntity.Fields.id).eq(id))
                .where(u -> u.get(UserEntity.Fields.email).isNull());
        return repository.one(query);
    }

    @Override
    public boolean existsByEmail(String email) {
        Query<UserEntity> query =
                Query.from(UserEntity.class).where(u -> u.get("email").eq(email));
        return repository.exists(query);
    }

    @Override
    public Long countUserByGroup(GroupAge group, String school) {
        Query<UserEntity> query = Query.from(UserEntity.class);
        if (!ObjectUtils.isEmpty(group)) {
            switch (group) {
                case Young ->
                    query = query.where(u ->
                            u.get(UserEntity.Fields.age).lt(18));
                case Adult ->
                    query = query.where(u -> u.and(
                            u.get(UserEntity.Fields.age).gte(18),
                            u.get(UserEntity.Fields.age).lte(30)));
                case Old ->
                    query = query.where(u ->
                            u.get(UserEntity.Fields.age).gt(30));
            }
        }
        if (StringUtils.hasText(school)) {
            query = query.where(u -> u.get(UserEntity.Fields.email).ilike("%" + school + "%"));
        }
        return repository.count(query);
    }
}
