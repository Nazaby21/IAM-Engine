package io.sala.krob_krong.iam.mapper;

import io.sala.krob_krong.iam.dto.PermissionResponse;
import io.sala.krob_krong.iam.entity.PermissionEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponse toResponse(PermissionEntity entity);

    List<PermissionResponse> toResponseList(List<PermissionEntity> entities);
}
