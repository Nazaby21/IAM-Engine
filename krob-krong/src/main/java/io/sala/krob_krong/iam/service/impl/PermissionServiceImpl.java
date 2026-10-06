package io.sala.krob_krong.iam.service.impl;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.iam.dto.PermissionResponse;
import io.sala.krob_krong.iam.entity.PermissionEntity;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.iam.mapper.PermissionMapper;
import io.sala.krob_krong.iam.repository.PermissionRepository;
import io.sala.krob_krong.iam.security.AccessPolicyService;
import io.sala.krob_krong.iam.service.PermissionService;
import io.sala.krob_krong.iam.specification.PermissionSpecification;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final AccessPolicyService accessPolicy;

    @Override
    public PageResponse<PermissionResponse> listPermissions(String search, PageRequest pageRequest) {
        Specification<PermissionEntity> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            spec = spec.and(PermissionSpecification.codeLike(search));
        }
        Page<PermissionResponse> page = permissionRepository
                .findAll(spec, pageRequest.toSpring(Sort.by("code")))
                .map(permissionMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public PermissionResponse getPermission(String code) {
        PermissionEntity entity = permissionRepository
                .findById(code)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.PERMISSION_NOT_FOUND));
        return permissionMapper.toResponse(entity);
    }

    @Override
    public Set<String> effectivePermissions(UUID userId, UUID schoolId, UUID branchId) {
        return accessPolicy.effectivePermissions(userId, schoolId, branchId);
    }
}
