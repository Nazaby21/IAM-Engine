package io.sala.krob_krong.iam.specification;

import io.sala.krob_krong.iam.entity.UserEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserSpecification {

    public static Specification<UserEntity> byEmail(String email) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get(UserEntity.Fields.email)), email.toLowerCase());
    }

    public static Specification<UserEntity> byStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get(UserEntity.Fields.status), status);
    }

    public static Specification<UserEntity> isPlatformAdmin() {
        return (root, query, cb) -> cb.isTrue(root.get(UserEntity.Fields.platformAdmin));
    }

    public static Specification<UserEntity> emailVerified() {
        return (root, query, cb) -> cb.isTrue(root.get(UserEntity.Fields.emailVerified));
    }

    public static Specification<UserEntity> displayNameLike(String fragment) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(UserEntity.Fields.displayName)), "%" + fragment.toLowerCase() + "%");
    }
}
