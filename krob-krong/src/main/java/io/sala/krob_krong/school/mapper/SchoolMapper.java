package io.sala.krob_krong.school.mapper;

import io.sala.krob_krong.school.dto.*;
import io.sala.krob_krong.school.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SchoolMapper {
    @Mapping(target = "logoUrl", expression = "java(imageUrl(school.getId(), school.getLogoAssetId()))")
    @Mapping(target = "coverUrl", expression = "java(imageUrl(school.getId(), school.getCoverAssetId()))")
    SchoolResponse toResponse(SchoolEntity school);

    SchoolSummaryResponse toSummary(SchoolEntity school);

    SchoolReviewResponse toReview(SchoolReviewEntity review);

    default String imageUrl(java.util.UUID schoolId, java.util.UUID assetId) {
        return assetId == null ? null : "/api/schools/" + schoolId + "/media/" + assetId + "/content";
    }
}
