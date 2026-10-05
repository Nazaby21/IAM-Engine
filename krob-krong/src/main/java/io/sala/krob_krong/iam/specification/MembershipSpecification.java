package io.sala.krob_krong.iam.specification;

import io.sala.krob_krong.iam.entity.MembershipEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MembershipSpecification {

    public static Specification<MembershipEntity> byUserId(String userId) {
        return (root, query, cb) -> cb.equal(root.get(MembershipEntity.Fields.user).get("id"), userId);
    }

    public static Specification<MembershipEntity> byTenantId(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get(MembershipEntity.Fields.tenant).get("id"), tenantId);
    }

    public static Specification<MembershipEntity> byStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get(MembershipEntity.Fields.status), status);
    }

    public static Specification<MembershipEntity> isOwner() {
        return (root, query, cb) -> cb.isTrue(root.get(MembershipEntity.Fields.owner));
    }

    public static Specification<MembershipEntity> active() {
        return byStatus("active");
    }

    public static Specification<MembershipEntity> byUserIdAndTenantId(String userId, String tenantId) {
        return byUserId(userId).and(byTenantId(tenantId));
    }
}
