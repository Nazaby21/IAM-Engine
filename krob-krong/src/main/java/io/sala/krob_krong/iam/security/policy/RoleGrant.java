package io.sala.krob_krong.iam.security.policy;

import java.util.UUID;

public record RoleGrant(UUID roleId, UUID schoolId, UUID branchId, boolean support) {

    public static RoleGrant global(UUID roleId) {
        return new RoleGrant(roleId, null, null, false);
    }

    public static RoleGrant inSchool(UUID roleId, UUID schoolId, UUID branchId) {
        return new RoleGrant(roleId, schoolId, branchId, false);
    }

    public static RoleGrant supportSession(UUID roleId, UUID schoolId) {
        return new RoleGrant(roleId, schoolId, null, true);
    }
}
