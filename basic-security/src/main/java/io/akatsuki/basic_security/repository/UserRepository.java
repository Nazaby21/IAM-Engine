package io.akatsuki.basic_security.repository;

import io.akatsuki.basic_security.common.enums.GroupAge;
import io.akatsuki.basic_security.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity , String > {
    boolean existsByEmail(String email);

    @Query(value = """
    SELECT COUNT(*)
    FROM users u
    WHERE
        (:groupAge IS NULL
            OR (:groupAge = 'Young' AND u.age < 18)
            OR (:groupAge = 'Adult' AND u.age BETWEEN 18 AND 30)
            OR (:groupAge = 'Old' AND u.age > 30)
        )
    AND
        (:school IS NULL OR u.email ILIKE CONCAT('%', :school, '%'))
    
    """, nativeQuery = true)
    Long countUserByGroup(@Param("groupAge") GroupAge groupAge, @Param("school") String school);
}
