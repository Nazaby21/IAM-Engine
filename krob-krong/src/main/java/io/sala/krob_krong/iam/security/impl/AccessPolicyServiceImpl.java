package io.sala.krob_krong.iam.security.impl;

import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.branch.entity.BranchEntity;
import io.sala.krob_krong.branch.repository.BranchRepository;
import io.sala.krob_krong.iam.entity.UserRoleEntity;
import io.sala.krob_krong.iam.repository.UserRoleRepository;
import io.sala.krob_krong.iam.security.AccessPolicyService;
import io.sala.krob_krong.iam.security.policy.AccessContext;
import io.sala.krob_krong.iam.security.policy.GrantContext;
import io.sala.krob_krong.iam.security.policy.RoleCatalog;
import io.sala.krob_krong.iam.security.policy.RoleCatalogProvider;
import io.sala.krob_krong.iam.security.policy.RoleGrant;
import io.sala.krob_krong.iam.specification.UserRoleSpecification;
import io.sala.krob_krong.school.entity.SchoolEntity;
import io.sala.krob_krong.school.repository.SchoolRepository;
import io.sala.krob_krong.support.entity.SupportAccessEntity;
import io.sala.krob_krong.support.repository.SupportAccessRepository;
import io.sala.krob_krong.support.specification.SupportAccessSpecification;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessPolicyServiceImpl implements AccessPolicyService {

    private static final String SUPPORT_START = "support:start";
    private static final Set<String> SCHOOL_STATUSES_WITHOUT_GRANTING = Set.of("suspended", "rejected");

    private final RoleCatalogProvider catalogProvider;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final BranchRepository branchRepository;
    private final UserRoleRepository userRoleRepository;
    private final SupportAccessRepository supportAccessRepository;

    @Override
    public boolean canAccess(UUID userId, String permission, UUID schoolId, UUID branchId, UUID targetUserId) {
        return accessContext(userId, schoolId, branchId).permits(permission, targetUserId);
    }

    @Override
    public Set<String> effectivePermissions(UUID userId, UUID schoolId, UUID branchId) {
        return accessContext(userId, schoolId, branchId).effectivePermissions();
    }

    @Override
    public boolean canGrantRole(UUID granterId, UUID roleId, UUID schoolId, UUID branchId) {
        return grantContext(granterId).mayGrant(roleId, schoolId, branchId);
    }

    @Override
    public Set<UUID> grantableRoleIds(UUID granterId, UUID schoolId, UUID branchId) {
        return grantContext(granterId).grantableRoles(schoolId, branchId);
    }

    @Override
    public boolean isSensitive(String permission) {
        return catalogProvider.get().isSensitive(permission);
    }

    private AccessContext accessContext(UUID userId, UUID schoolId, UUID branchId) {
        RoleCatalog catalog = catalogProvider.get();
        String schoolStatus = schoolId == null
                ? null
                : schoolRepository
                        .findById(schoolId)
                        .map(SchoolEntity::getStatus)
                        .orElse(null);
        String branchStatus = branchId == null || schoolStatus == null
                ? null
                : branchRepository
                        .findById(branchId)
                        .filter(branch -> schoolId.equals(branch.getSchoolId()))
                        .map(BranchEntity::getStatus)
                        .orElse(null);

        UUID activeUserId = activePersonId(userId);
        List<RoleGrant> grants = new ArrayList<>();
        catalog.rolesOfKind(RoleCatalog.KIND_ANYONE).forEach(roleId -> grants.add(RoleGrant.global(roleId)));
        if (activeUserId != null) {
            catalog.rolesOfKind(RoleCatalog.KIND_AUTHENTICATED).forEach(roleId -> grants.add(RoleGrant.global(roleId)));
            List<UserRoleEntity> validGrants = validGrants(activeUserId);
            Map<UUID, String> branchStatuses = branchStatuses(validGrants);
            validGrants.stream()
                    .filter(grant -> grant.getBranchId() == null
                            || AccessContext.BRANCH_ACTIVE.equals(branchStatuses.get(grant.getBranchId())))
                    .map(grant -> RoleGrant.inSchool(grant.getRoleId(), grant.getSchoolId(), grant.getBranchId()))
                    .forEach(grants::add);
            if (validGrants.stream().anyMatch(grant -> catalog.grants(grant.getRoleId(), SUPPORT_START))) {
                grants.addAll(supportSessionGrants(catalog, activeUserId));
            }
        }
        return new AccessContext(catalog, activeUserId, schoolId, schoolStatus, branchId, branchStatus, grants);
    }

    private GrantContext grantContext(UUID granterId) {
        RoleCatalog catalog = catalogProvider.get();
        UUID activeGranterId = activePersonId(granterId);
        if (activeGranterId == null) {
            return new GrantContext(catalog, List.of());
        }
        List<UserRoleEntity> validGrants = validGrants(activeGranterId);
        Map<UUID, String> schoolStatuses = schoolStatuses(validGrants);
        Map<UUID, String> branchStatuses = branchStatuses(validGrants);
        List<RoleGrant> usableGrants = validGrants.stream()
                .filter(grant -> grant.getSchoolId() == null
                        || (allowsGranting(schoolStatuses.get(grant.getSchoolId()))
                                && (grant.getBranchId() == null
                                        || AccessContext.BRANCH_ACTIVE.equals(
                                                branchStatuses.get(grant.getBranchId())))))
                .map(grant -> RoleGrant.inSchool(grant.getRoleId(), grant.getSchoolId(), grant.getBranchId()))
                .toList();
        return new GrantContext(catalog, usableGrants);
    }

    private UUID activePersonId(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository
                .findById(userId)
                .filter(UserEntity::isActive)
                .filter(UserEntity::isPerson)
                .map(UserEntity::getId)
                .orElse(null);
    }

    private List<UserRoleEntity> validGrants(UUID userId) {
        return userRoleRepository.findAll(
                UserRoleSpecification.byUserId(userId).and(UserRoleSpecification.activeNow()));
    }

    private List<RoleGrant> supportSessionGrants(RoleCatalog catalog, UUID userId) {
        List<UUID> supportRoles = catalog.rolesOfKind(RoleCatalog.KIND_SUPPORT);
        return supportAccessRepository
                .findAll(SupportAccessSpecification.byUserId(userId).and(SupportAccessSpecification.active()))
                .stream()
                .filter(SupportAccessEntity::isActive)
                .flatMap(session ->
                        supportRoles.stream().map(roleId -> RoleGrant.supportSession(roleId, session.getSchoolId())))
                .toList();
    }

    private Map<UUID, String> schoolStatuses(List<UserRoleEntity> grants) {
        Set<UUID> schoolIds = grants.stream()
                .map(UserRoleEntity::getSchoolId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (schoolIds.isEmpty()) {
            return Map.of();
        }
        return schoolRepository.findAllById(schoolIds).stream()
                .collect(Collectors.toMap(SchoolEntity::getId, SchoolEntity::getStatus));
    }

    private Map<UUID, String> branchStatuses(List<UserRoleEntity> grants) {
        Set<UUID> branchIds = grants.stream()
                .map(UserRoleEntity::getBranchId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (branchIds.isEmpty()) {
            return Map.of();
        }
        return branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(BranchEntity::getId, BranchEntity::getStatus));
    }

    private static boolean allowsGranting(String schoolStatus) {
        return schoolStatus != null && !SCHOOL_STATUSES_WITHOUT_GRANTING.contains(schoolStatus);
    }
}
