package io.sala.krob_krong.iam.service;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.iam.dto.PermissionResponse;
import java.util.Set;
import java.util.UUID;

public interface PermissionService {

    PageResponse<PermissionResponse> listPermissions(String search, PageRequest pageRequest);

    PermissionResponse getPermission(String code);

    Set<String> effectivePermissions(UUID userId, UUID schoolId, UUID branchId);
}
