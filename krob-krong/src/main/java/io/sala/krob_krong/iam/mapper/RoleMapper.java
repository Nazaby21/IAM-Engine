package io.sala.krob_krong.iam.mapper;

import io.sala.krob_krong.iam.dto.RolePermissionResponse;
import io.sala.krob_krong.iam.dto.RoleResponse;
import io.sala.krob_krong.iam.entity.RoleEntity;
import io.sala.krob_krong.iam.entity.RolePermissionEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse toResponse(RoleEntity entity);

    List<RoleResponse> toResponseList(List<RoleEntity> entities);

    RolePermissionResponse toPermissionResponse(RolePermissionEntity entity);

    List<RolePermissionResponse> toPermissionResponseList(List<RolePermissionEntity> entities);
}
