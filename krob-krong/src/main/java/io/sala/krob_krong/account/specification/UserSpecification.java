package io.sala.krob_krong.account.specification;

import io.sala.krob_krong.account.entity.UserEntity;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserSpecification {

    public static Specification<UserEntity> byEmail(String email) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get(UserEntity.Fields.email)), email.toLowerCase(Locale.ROOT));
    }

    public static Specification<UserEntity> byPhone(String phone) {
        return (root, query, cb) -> cb.equal(root.get(UserEntity.Fields.phone), phone);
    }

    public static Specification<UserEntity> byKind(String kind) {
        return (root, query, cb) -> cb.equal(root.get(UserEntity.Fields.kind), kind);
    }

    public static Specification<UserEntity> persons() {
        return byKind("person");
    }

    public static Specification<UserEntity> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get(UserEntity.Fields.active));
    }

    public static Specification<UserEntity> isVerified() {
        return (root, query, cb) -> cb.isNotNull(root.get(UserEntity.Fields.verifiedAt));
    }

    public static Specification<UserEntity> hasMfa() {
        return (root, query, cb) -> cb.isNotNull(root.get(UserEntity.Fields.mfaEnabledAt));
    }

    public static Specification<UserEntity> displayNameLike(String fragment) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(UserEntity.Fields.displayName)), "%" + fragment.toLowerCase() + "%");
    }

    public static Specification<UserEntity> byId(UUID id) {
        return (root, query, cb) -> cb.equal(root.get(UserEntity.Fields.id), id);
    }
}
