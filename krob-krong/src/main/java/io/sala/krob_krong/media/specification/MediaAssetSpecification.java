package io.sala.krob_krong.media.specification;

import io.sala.krob_krong.media.entity.MediaAssetEntity;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MediaAssetSpecification {

    public static Specification<MediaAssetEntity> bySchoolId(UUID schoolId) {
        return (root, query, cb) -> cb.equal(root.get(MediaAssetEntity.Fields.schoolId), schoolId);
    }

    public static Specification<MediaAssetEntity> byBranchId(UUID branchId) {
        return (root, query, cb) -> cb.equal(root.get(MediaAssetEntity.Fields.branchId), branchId);
    }

    public static Specification<MediaAssetEntity> byUploadedBy(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get(MediaAssetEntity.Fields.uploadedBy), userId);
    }

    public static Specification<MediaAssetEntity> byKind(String kind) {
        return (root, query, cb) -> cb.equal(root.get(MediaAssetEntity.Fields.kind), kind);
    }

    public static Specification<MediaAssetEntity> byPurpose(String purpose) {
        return (root, query, cb) -> cb.equal(root.get(MediaAssetEntity.Fields.purpose), purpose);
    }

    public static Specification<MediaAssetEntity> byStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get(MediaAssetEntity.Fields.status), status);
    }

    public static Specification<MediaAssetEntity> byVisibility(String visibility) {
        return (root, query, cb) -> cb.equal(root.get(MediaAssetEntity.Fields.visibility), visibility);
    }

    public static Specification<MediaAssetEntity> ready() {
        return byStatus("ready");
    }

    public static Specification<MediaAssetEntity> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get(MediaAssetEntity.Fields.deletedAt));
    }

    public static Specification<MediaAssetEntity> personal() {
        return (root, query, cb) -> cb.isNull(root.get(MediaAssetEntity.Fields.schoolId));
    }
}
