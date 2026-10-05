package io.sala.krob_krong.branch.specification;

import io.sala.krob_krong.branch.entity.BranchEntity;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BranchSpecification {

    public static Specification<BranchEntity> bySchoolId(UUID schoolId) {
        return (root, query, cb) -> cb.equal(root.get(BranchEntity.Fields.schoolId), schoolId);
    }

    public static Specification<BranchEntity> byStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get(BranchEntity.Fields.status), status);
    }

    public static Specification<BranchEntity> active() {
        return byStatus("active");
    }

    public static Specification<BranchEntity> isDefault() {
        return (root, query, cb) -> cb.isTrue(root.get(BranchEntity.Fields.defaultBranch));
    }

    public static Specification<BranchEntity> defaultOfSchool(UUID schoolId) {
        return bySchoolId(schoolId).and(isDefault());
    }

    public static Specification<BranchEntity> nameLike(String fragment) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(BranchEntity.Fields.name)), "%" + fragment.toLowerCase() + "%");
    }

    public static Specification<BranchEntity> byCountryCode(String countryCode) {
        return (root, query, cb) -> cb.equal(root.get(BranchEntity.Fields.countryCode), countryCode);
    }

    public static Specification<BranchEntity> byProvince(String province) {
        return (root, query, cb) -> cb.equal(root.get(BranchEntity.Fields.province), province);
    }
}
