package io.sala.krob_krong.iam.security.policy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record AccessContext(
        RoleCatalog catalog,
        UUID userId,
        UUID schoolId,
        String schoolStatus,
        UUID branchId,
        String branchStatus,
        List<RoleGrant> grants) {

    public static final String SCOPE_PUBLIC = "public";
    public static final String SCOPE_OWN = "own";
    public static final String SCOPE_TENANT = "tenant";
    public static final String SCOPE_PLATFORM = "platform";

    public static final String SCHOOL_APPROVED = "approved";
    public static final String SCHOOL_SUSPENDED = "suspended";
    public static final String BRANCH_ACTIVE = "active";

    public boolean permits(String permission, UUID targetUserId) {
        if (!targetExists()) {
            return false;
        }
        for (RoleGrant grant : grants) {
            for (String scope : catalog.scopes(grant.roleId(), permission)) {
                if (scopeAllows(scope, grant, permission, targetUserId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<String> effectivePermissions() {
        return catalog.permissionCodes().stream()
                .filter(permission -> permits(permission, null) || permits(permission, userId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean targetExists() {
        return (schoolId == null || schoolStatus != null) && (branchId == null || branchStatus != null);
    }

    private boolean scopeAllows(String scope, RoleGrant grant, String permission, UUID targetUserId) {
        return switch (scope) {
            case SCOPE_PLATFORM -> true;
            case SCOPE_OWN -> userId != null && userId.equals(targetUserId);
            case SCOPE_PUBLIC ->
                targetUserId == null
                        && SCHOOL_APPROVED.equals(schoolStatus)
                        && (branchId == null || BRANCH_ACTIVE.equals(branchStatus));
            case SCOPE_TENANT -> tenantAllows(grant, permission);
            default -> false;
        };
    }

    private boolean tenantAllows(RoleGrant grant, String permission) {
        if (grant.schoolId() == null || !grant.schoolId().equals(schoolId)) {
            return false;
        }
        if (SCHOOL_SUSPENDED.equals(schoolStatus) && !grant.support()) {
            return false;
        }
        return grant.branchId() == null
                || grant.branchId().equals(branchId)
                || (branchId == null && isReadOnly(permission));
    }

    private static boolean isReadOnly(String permission) {
        return permission.endsWith(":read") || permission.endsWith(":view");
    }
}
