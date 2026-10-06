package io.sala.krob_krong.iam.service;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.iam.dto.RoleDetailResponse;
import io.sala.krob_krong.iam.dto.RoleResponse;
import java.util.List;
import java.util.UUID;

public interface RoleService {

    PageResponse<RoleResponse> listRoles(String kind, PageRequest pageRequest);

    RoleDetailResponse getRole(UUID roleId);

    List<RoleResponse> getGrantableRoles(UUID granterId, UUID schoolId, UUID branchId);
}
