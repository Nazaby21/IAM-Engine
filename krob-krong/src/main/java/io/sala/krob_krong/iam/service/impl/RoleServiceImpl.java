package io.sala.krob_krong.iam.service.impl;

import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.iam.dto.RoleDetailResponse;
import io.sala.krob_krong.iam.dto.RoleResponse;
import io.sala.krob_krong.iam.entity.RoleEntity;
import io.sala.krob_krong.iam.entity.RoleGrantRuleEntity;
import io.sala.krob_krong.iam.entity.RolePermissionEntity;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.iam.mapper.RoleMapper;
import io.sala.krob_krong.iam.repository.RoleGrantRuleRepository;
import io.sala.krob_krong.iam.repository.RolePermissionRepository;
import io.sala.krob_krong.iam.repository.RoleRepository;
import io.sala.krob_krong.iam.security.AccessPolicyService;
import io.sala.krob_krong.iam.service.RoleService;
import io.sala.krob_krong.iam.specification.RolePermissionSpecification;
import io.sala.krob_krong.iam.specification.RoleSpecification;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleGrantRuleRepository roleGrantRuleRepository;
    private final AccessPolicyService accessPolicy;
    private final RoleMapper roleMapper;

    @Override
    public PageResponse<RoleResponse> listRoles(String kind, PageRequest pageRequest) {
        Specification<RoleEntity> spec = (root, query, cb) -> cb.conjunction();
        if (kind != null) {
            spec = spec.and(RoleSpecification.byKind(kind));
        }
        Page<RoleResponse> page = roleRepository
                .findAll(spec, pageRequest.toSpring(Sort.by("name")))
                .map(roleMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public RoleDetailResponse getRole(UUID roleId) {
        RoleEntity role =
                roleRepository.findById(roleId).orElseThrow(() -> new BusinessException(IAMErrorCode.ROLE_NOT_FOUND));

        List<RolePermissionEntity> permissions =
                rolePermissionRepository.findAll(RolePermissionSpecification.byRoleId(roleId));

        List<UUID> grantableIds = roleGrantRuleRepository
                .findAll((root, query, cb) -> cb.equal(root.get(RoleGrantRuleEntity.Fields.granterRoleId), roleId))
                .stream()
                .map(RoleGrantRuleEntity::getGranteeRoleId)
                .toList();

        return RoleDetailResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .kind(role.getKind())
                .allowsBranch(role.isAllowsBranch())
                .preApproval(role.isPreApproval())
                .permissions(roleMapper.toPermissionResponseList(permissions))
                .grantableRoleIds(grantableIds)
                .build();
    }

    @Override
    public List<RoleResponse> getGrantableRoles(UUID granterId, UUID schoolId, UUID branchId) {
        Set<UUID> grantableIds = accessPolicy.grantableRoleIds(granterId, schoolId, branchId);
        if (grantableIds.isEmpty()) {
            return List.of();
        }
        return roleRepository.findAllById(grantableIds).stream()
                .sorted(Comparator.comparing(RoleEntity::getName))
                .map(roleMapper::toResponse)
                .toList();
    }
}
