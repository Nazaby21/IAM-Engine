package io.sala.krob_krong.iam.service.impl;

import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.iam.dto.GrantRoleRequest;
import io.sala.krob_krong.iam.dto.MemberResponse;
import io.sala.krob_krong.iam.dto.UserRoleResponse;
import io.sala.krob_krong.iam.entity.RoleEntity;
import io.sala.krob_krong.iam.entity.UserRoleEntity;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.iam.mapper.UserRoleMapper;
import io.sala.krob_krong.iam.repository.RoleRepository;
import io.sala.krob_krong.iam.repository.UserRoleRepository;
import io.sala.krob_krong.iam.security.AccessPolicyService;
import io.sala.krob_krong.iam.service.UserRoleService;
import io.sala.krob_krong.iam.specification.UserRoleSpecification;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AccessPolicyService accessPolicy;
    private final UserRoleMapper userRoleMapper;

    @Override
    @Transactional
    public UserRoleResponse grantRole(UUID schoolId, GrantRoleRequest request, UUID actorId) {
        RoleEntity role = roleRepository
                .findById(request.getRoleId())
                .orElseThrow(() -> new BusinessException(IAMErrorCode.ROLE_NOT_FOUND));

        if (!userRepository.existsById(request.getUserId())) {
            throw new BusinessException(IAMErrorCode.USER_NOT_FOUND);
        }
        if (request.getUserId().equals(actorId)) {
            throw new BusinessException(IAMErrorCode.ROLE_GRANT_NOT_ALLOWED, "Nobody can grant themselves a role");
        }
        requireCanGrant(actorId, request.getRoleId(), schoolId, request.getBranchId());

        boolean alreadyHeld = userRoleRepository.exists(UserRoleSpecification.byUserId(request.getUserId())
                .and(UserRoleSpecification.byRoleId(request.getRoleId()))
                .and(UserRoleSpecification.bySchoolId(schoolId))
                .and(
                        request.getBranchId() != null
                                ? UserRoleSpecification.byBranchId(request.getBranchId())
                                : UserRoleSpecification.schoolWide())
                .and(UserRoleSpecification.notRevoked())
                .and(UserRoleSpecification.validAt(Instant.now())));
        if (alreadyHeld) {
            throw new BusinessException(IAMErrorCode.ROLE_ALREADY_HELD);
        }

        UserRoleEntity grant = new UserRoleEntity();
        grant.setUserId(request.getUserId());
        grant.setRoleId(request.getRoleId());
        grant.setSchoolId(schoolId);
        grant.setBranchId(request.getBranchId());
        grant.setSource("invite");
        grant.setValidFrom(Instant.now());
        grant.setValidTo(request.getValidTo());
        grant.setGrantedBy(actorId);
        grant.setGrantedAt(Instant.now());

        UserRoleEntity saved = userRoleRepository.save(grant);
        saved.setRole(role);
        return userRoleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserRoleResponse acceptInvitation(UUID userRoleId, UUID actorId) {
        UserRoleEntity grant = userRoleRepository
                .findById(userRoleId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.INVITATION_NOT_FOUND));

        if (!grant.getUserId().equals(actorId)) {
            throw new BusinessException(IAMErrorCode.INVITATION_NOT_FOUND);
        }
        if (grant.getAcceptedAt() != null) {
            throw new BusinessException(IAMErrorCode.INVITATION_ALREADY_ACCEPTED);
        }
        if (grant.getRevokedAt() != null) {
            throw new BusinessException(IAMErrorCode.INVITATION_NOT_FOUND);
        }

        grant.setAcceptedAt(Instant.now());
        UserRoleEntity saved = userRoleRepository.save(grant);
        return userRoleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserRoleResponse revokeGrant(UUID userRoleId, String reason, UUID actorId) {
        UserRoleEntity grant = userRoleRepository
                .findById(userRoleId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.INVITATION_NOT_FOUND));

        if (grant.getRevokedAt() != null) {
            throw new BusinessException(IAMErrorCode.INVITATION_NOT_FOUND);
        }

        if (!grant.getUserId().equals(actorId)) {
            requireCanGrant(actorId, grant.getRoleId(), grant.getSchoolId(), grant.getBranchId());
        }

        grant.setRevokedAt(Instant.now());
        grant.setRevokedBy(actorId);
        grant.setRevokeReason(reason);
        grant.setValidTo(Instant.now());
        UserRoleEntity saved = userRoleRepository.save(grant);
        return userRoleMapper.toResponse(saved);
    }

    @Override
    public List<UserRoleResponse> listUserRoles(UUID userId, UUID schoolId) {
        List<UserRoleEntity> grants = userRoleRepository.findAll(
                UserRoleSpecification.byUserId(userId)
                        .and(UserRoleSpecification.bySchoolId(schoolId))
                        .and(UserRoleSpecification.notRevoked())
                        .and(UserRoleSpecification.fetchRole()),
                Sort.by(Sort.Direction.DESC, UserRoleEntity.Fields.grantedAt));
        return userRoleMapper.toResponseList(grants);
    }

    @Override
    public PageResponse<MemberResponse> listSchoolMembers(UUID schoolId, PageRequest pageRequest) {
        Page<UserRoleEntity> grantPage = userRoleRepository.findAll(
                UserRoleSpecification.bySchoolId(schoolId).and(UserRoleSpecification.notRevoked()),
                pageRequest.toSpring(Sort.by(UserRoleEntity.Fields.userId)));

        Set<UUID> userIds =
                grantPage.getContent().stream().map(UserRoleEntity::getUserId).collect(Collectors.toSet());

        Map<UUID, UserEntity> users =
                userRepository.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, u -> u));

        List<UserRoleEntity> allGrants = userRoleRepository.findAll(UserRoleSpecification.bySchoolId(schoolId)
                .and(UserRoleSpecification.notRevoked())
                .and((root, query, cb) -> root.get(UserRoleEntity.Fields.userId).in(userIds))
                .and(UserRoleSpecification.fetchRole()));

        Map<UUID, List<UserRoleEntity>> grantsByUser =
                allGrants.stream().collect(Collectors.groupingBy(UserRoleEntity::getUserId));

        List<MemberResponse> members = userIds.stream()
                .map(uid -> {
                    UserEntity user = users.get(uid);
                    List<UserRoleEntity> userGrants = grantsByUser.getOrDefault(uid, List.of());
                    return MemberResponse.builder()
                            .userId(uid)
                            .displayName(user != null ? user.getDisplayName() : null)
                            .email(user != null ? user.getEmail() : null)
                            .roles(userRoleMapper.toResponseList(userGrants))
                            .build();
                })
                .toList();

        Page<MemberResponse> memberPage = new org.springframework.data.domain.PageImpl<>(
                members, grantPage.getPageable(), grantPage.getTotalElements());
        return PageResponse.from(memberPage);
    }

    @Override
    public List<UserRoleResponse> myInvitations(UUID userId) {
        List<UserRoleEntity> pending = userRoleRepository.findAll(
                UserRoleSpecification.byUserId(userId)
                        .and(UserRoleSpecification.pendingInvitation())
                        .and(UserRoleSpecification.notRevoked())
                        .and(UserRoleSpecification.fetchRole()),
                Sort.by(Sort.Direction.DESC, UserRoleEntity.Fields.grantedAt));
        return userRoleMapper.toResponseList(pending);
    }

    private void requireCanGrant(UUID actorId, UUID roleId, UUID schoolId, UUID branchId) {
        if (!accessPolicy.canGrantRole(actorId, roleId, schoolId, branchId)) {
            throw new BusinessException(IAMErrorCode.ROLE_GRANT_NOT_ALLOWED);
        }
    }
}
