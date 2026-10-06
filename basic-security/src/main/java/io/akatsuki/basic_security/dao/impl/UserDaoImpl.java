package io.akatsuki.basic_security.dao.impl;

import io.acmwchsd.data.query.Query;
import io.acmwchsd.data.repository.ACMRepository;
import io.akatsuki.basic_security.common.enums.AgeType;
import io.akatsuki.basic_security.dao.UserDao;
import io.akatsuki.basic_security.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

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
    public List<UserEntity> findAllUsers() {

        Query<UserEntity> query =
                Query.from(UserEntity.class);

        return repository.list(query);
    }

    @Override
    public List<UserEntity> findByAgeType(AgeType ageType) {
        if (ageType == null) {
            return List.of();
        }

        Query<UserEntity> query = switch (ageType) {
            case YOUNG -> Query.from(UserEntity.class)
                    .where(u -> u.get(UserEntity.Fields.age).lt(18));
            case ADULT -> Query.from(UserEntity.class)
                    .where(u -> u.get(UserEntity.Fields.age).gte(18))
                    .where(u -> u.get(UserEntity.Fields.age).lte(35));
            case OLD -> Query.from(UserEntity.class)
                    .where(u -> u.get(UserEntity.Fields.age).gt(35));
        };

        return repository.list(query);
    }

    @Override
    public List<UserEntity> findByAgeTypeOrSchool(AgeType ageType, String school) {

        Query<UserEntity> query = Query.from(UserEntity.class);

        if (ageType != null) {
            switch (ageType) {
                case YOUNG -> Query.from(UserEntity.class)
                        .where(u -> u.get(UserEntity.Fields.age).lt(18));
                case ADULT ->  Query.from(UserEntity.class)
                        .where(u -> u.get(UserEntity.Fields.age).gte(18))
                        .where(u -> u.get(UserEntity.Fields.age).lt(35));
                case OLD -> Query.from(UserEntity.class)
                        .where(u -> u.get(UserEntity.Fields.age).lte(35));
            }
        }
        if (school != null && !school.isBlank()) {
            query = query.where(u -> u.get(UserEntity.Fields.email).like("%" + school + "%"));
        }

        return repository.list(query);
    }
}