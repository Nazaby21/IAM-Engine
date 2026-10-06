package io.sala.krob_krong.iam.security.policy;

import static io.sala.krob_krong.iam.security.policy.SeedRoles.INSTRUCTOR;
import static io.sala.krob_krong.iam.security.policy.SeedRoles.MEMBER;
import static io.sala.krob_krong.iam.security.policy.SeedRoles.SCHOOL_ADMIN;
import static io.sala.krob_krong.iam.security.policy.SeedRoles.SCHOOL_OWNER;
import static io.sala.krob_krong.iam.security.policy.SeedRoles.SUPER_ADMIN;
import static io.sala.krob_krong.iam.security.policy.SeedRoles.SUPPORT_VIEWER;
import static io.sala.krob_krong.iam.security.policy.SeedRoles.VISITOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessPolicyRulesTest {

    private static final UUID SCHOOL_A = UUID.randomUUID();
    private static final UUID SCHOOL_B = UUID.randomUUID();
    private static final UUID BRANCH_1 = UUID.randomUUID();
    private static final UUID BRANCH_2 = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID OTHER_USER = UUID.randomUUID();

    private final RoleCatalog catalog = SeedRoles.catalog();

    @Test
    void anonymousVisitorSeesOnlyApprovedSchoolsAndActiveBranches() {
        assertThat(anonymous(SCHOOL_A, "approved", null, null).permits("school.profile:read", null))
                .isTrue();
        assertThat(anonymous(SCHOOL_A, "draft", null, null).permits("school.profile:read", null))
                .isFalse();
        assertThat(anonymous(SCHOOL_A, "approved", BRANCH_1, "draft").permits("branch:read", null))
                .isFalse();
        assertThat(anonymous(SCHOOL_A, "approved", null, null).permits("member:read", null))
                .isFalse();
    }

    @Test
    void tenantGrantIsLimitedToItsOwnSchool() {
        List<RoleGrant> grants = List.of(RoleGrant.inSchool(SCHOOL_ADMIN, SCHOOL_A, null));

        assertThat(context(grants, SCHOOL_A, "approved", null, null).permits("member:read", null))
                .isTrue();
        assertThat(context(grants, SCHOOL_B, "approved", null, null).permits("member:read", null))
                .isFalse();
    }

    @Test
    void suspendedSchoolBlocksMembersButNotSupportSessions() {
        List<RoleGrant> member = List.of(RoleGrant.inSchool(SCHOOL_ADMIN, SCHOOL_A, null));
        List<RoleGrant> support = List.of(RoleGrant.supportSession(SUPPORT_VIEWER, SCHOOL_A));

        assertThat(context(member, SCHOOL_A, "suspended", null, null).permits("member:read", null))
                .isFalse();
        assertThat(context(support, SCHOOL_A, "suspended", null, null).permits("member:read", null))
                .isTrue();
    }

    @Test
    void branchGrantActsOnlyInItsBranchButReadsSchoolWide() {
        List<RoleGrant> grants = List.of(RoleGrant.inSchool(INSTRUCTOR, SCHOOL_A, BRANCH_1));

        assertThat(context(grants, SCHOOL_A, "approved", BRANCH_1, "active").permits("media:upload", null))
                .isTrue();
        assertThat(context(grants, SCHOOL_A, "approved", BRANCH_2, "active").permits("media:upload", null))
                .isFalse();
        assertThat(context(grants, SCHOOL_A, "approved", null, null).permits("media:upload", null))
                .isFalse();
        assertThat(context(grants, SCHOOL_A, "approved", null, null).permits("branch:read", null))
                .isTrue();
    }

    @Test
    void ownScopeAppliesOnlyToTheUserThemself() {
        AccessContext context = context(List.of(RoleGrant.global(MEMBER)), SCHOOL_A, "approved", null, null);

        assertThat(context.permits("member:read", USER)).isTrue();
        assertThat(context.permits("member:read", OTHER_USER)).isFalse();
        assertThat(context.permits("member:read", null)).isFalse();
    }

    @Test
    void platformScopeAppliesEverywhere() {
        List<RoleGrant> grants = List.of(RoleGrant.global(SUPER_ADMIN));

        assertThat(context(grants, SCHOOL_A, "draft", null, null).permits("school:review", null))
                .isTrue();
        assertThat(context(grants, null, null, null, null).permits("support:start", null))
                .isTrue();
    }

    @Test
    void unknownSchoolOrBranchDeniesEvenPlatformGrants() {
        List<RoleGrant> grants = List.of(RoleGrant.global(SUPER_ADMIN));

        assertThat(context(grants, SCHOOL_A, null, null, null).permits("school:review", null))
                .isFalse();
        assertThat(context(grants, SCHOOL_A, "approved", BRANCH_1, null).permits("school:review", null))
                .isFalse();
    }

    @Test
    void effectivePermissionsCombinePublicOwnAndTenantGrants() {
        List<RoleGrant> grants = List.of(
                RoleGrant.global(VISITOR),
                RoleGrant.global(MEMBER),
                RoleGrant.inSchool(INSTRUCTOR, SCHOOL_A, BRANCH_1));

        assertThat(context(grants, SCHOOL_A, "approved", BRANCH_1, "active").effectivePermissions())
                .containsExactly(
                        "branch:read", "media.public:view", "media:upload", "member:read", "school.profile:read");
    }

    @Test
    void ownerGrantsAdminsOnlyInTheirOwnSchool() {
        GrantContext owner = grantContext(RoleGrant.inSchool(SCHOOL_OWNER, SCHOOL_A, null));

        assertThat(owner.mayGrant(SCHOOL_ADMIN, SCHOOL_A, null)).isTrue();
        assertThat(owner.mayGrant(SCHOOL_ADMIN, SCHOOL_A, BRANCH_1)).isTrue();
        assertThat(owner.mayGrant(SCHOOL_ADMIN, SCHOOL_B, null)).isFalse();
    }

    @Test
    void branchLimitedAdminGrantsInstructorsOnlyInTheirBranch() {
        GrantContext admin = grantContext(RoleGrant.inSchool(SCHOOL_ADMIN, SCHOOL_A, BRANCH_1));

        assertThat(admin.mayGrant(INSTRUCTOR, SCHOOL_A, BRANCH_1)).isTrue();
        assertThat(admin.mayGrant(INSTRUCTOR, SCHOOL_A, BRANCH_2)).isFalse();
        assertThat(admin.mayGrant(INSTRUCTOR, SCHOOL_A, null)).isFalse();
    }

    @Test
    void platformAdminGrantsOwnersInAnySchool() {
        GrantContext superAdmin = grantContext(RoleGrant.global(SUPER_ADMIN));

        assertThat(superAdmin.mayGrant(SCHOOL_OWNER, SCHOOL_A, null)).isTrue();
        assertThat(superAdmin.mayGrant(SCHOOL_OWNER, SCHOOL_B, null)).isTrue();
    }

    @Test
    void grantWithoutAGrantRuleIsRefused() {
        GrantContext admin = grantContext(RoleGrant.inSchool(SCHOOL_ADMIN, SCHOOL_A, null));

        assertThat(admin.mayGrant(SCHOOL_OWNER, SCHOOL_A, null)).isFalse();
        assertThat(admin.mayGrant(SCHOOL_ADMIN, SCHOOL_A, null)).isFalse();
    }

    @Test
    void grantableRolesRespectRoleKindAndBranchSupport() {
        GrantContext owner = grantContext(RoleGrant.inSchool(SCHOOL_OWNER, SCHOOL_A, null));
        GrantContext superAdmin = grantContext(RoleGrant.global(SUPER_ADMIN));

        assertThat(owner.grantableRoles(SCHOOL_A, null))
                .containsExactlyInAnyOrder(SCHOOL_OWNER, SCHOOL_ADMIN, INSTRUCTOR);
        assertThat(owner.grantableRoles(SCHOOL_A, BRANCH_1)).containsExactlyInAnyOrder(SCHOOL_ADMIN, INSTRUCTOR);
        assertThat(superAdmin.grantableRoles(null, null)).containsExactly(SUPER_ADMIN);
        assertThat(superAdmin.grantableRoles(SCHOOL_A, null)).containsExactly(SCHOOL_OWNER);
    }

    private AccessContext anonymous(UUID schoolId, String schoolStatus, UUID branchId, String branchStatus) {
        return new AccessContext(
                catalog, null, schoolId, schoolStatus, branchId, branchStatus, List.of(RoleGrant.global(VISITOR)));
    }

    private AccessContext context(
            List<RoleGrant> grants, UUID schoolId, String schoolStatus, UUID branchId, String branchStatus) {
        return new AccessContext(catalog, USER, schoolId, schoolStatus, branchId, branchStatus, grants);
    }

    private GrantContext grantContext(RoleGrant grant) {
        return new GrantContext(catalog, List.of(grant));
    }
}
