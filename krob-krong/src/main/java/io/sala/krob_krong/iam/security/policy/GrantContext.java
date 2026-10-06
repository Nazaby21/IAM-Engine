package io.sala.krob_krong.iam.security.policy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record GrantContext(RoleCatalog catalog, List<RoleGrant> grants) {

    public boolean mayGrant(UUID roleId, UUID schoolId, UUID branchId) {
        return grants.stream()
                .anyMatch(grant -> catalog.canGrant(grant.roleId(), roleId) && covers(grant, schoolId, branchId));
    }

    public Set<UUID> grantableRoles(UUID schoolId, UUID branchId) {
        String assignableKind = schoolId == null ? RoleCatalog.KIND_PLATFORM : RoleCatalog.KIND_SCHOOL;
        return catalog.roleIds().stream()
                .filter(roleId -> assignableKind.equals(catalog.kindOf(roleId)))
                .filter(roleId -> branchId == null || catalog.allowsBranch(roleId))
                .filter(roleId -> mayGrant(roleId, schoolId, branchId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean covers(RoleGrant grant, UUID schoolId, UUID branchId) {
        if (grant.schoolId() == null) {
            return true;
        }
        return grant.schoolId().equals(schoolId)
                && (grant.branchId() == null || grant.branchId().equals(branchId));
    }
}
