package io.sala.krob_krong.iam.mapper;

import io.sala.krob_krong.iam.dto.UserRoleResponse;
import io.sala.krob_krong.iam.entity.UserRoleEntity;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserRoleMapper {

    @Mapping(target = "roleCode", source = "role.code")
    @Mapping(target = "roleName", source = "role.name")
    @Mapping(target = "status", source = ".", qualifiedByName = "deriveStatus")
    UserRoleResponse toResponse(UserRoleEntity entity);

    List<UserRoleResponse> toResponseList(List<UserRoleEntity> entities);

    @Named("deriveStatus")
    default String deriveStatus(UserRoleEntity entity) {
        if (entity.getRevokedAt() != null) return "revoked";
        if (entity.getValidTo() != null && entity.getValidTo().isBefore(Instant.now())) return "expired";
        if (entity.getAcceptedAt() == null) return "pending";
        return "active";
    }
}
