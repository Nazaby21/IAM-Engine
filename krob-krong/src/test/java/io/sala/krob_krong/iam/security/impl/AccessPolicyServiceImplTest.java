package io.sala.krob_krong.iam.security.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.branch.entity.BranchEntity;
import io.sala.krob_krong.branch.repository.BranchRepository;
import io.sala.krob_krong.iam.entity.UserRoleEntity;
import io.sala.krob_krong.iam.repository.UserRoleRepository;
import io.sala.krob_krong.iam.security.policy.RoleCatalogProvider;
import io.sala.krob_krong.iam.security.policy.SeedRoles;
import io.sala.krob_krong.school.entity.SchoolEntity;
import io.sala.krob_krong.school.repository.SchoolRepository;
import io.sala.krob_krong.support.entity.SupportAccessEntity;
import io.sala.krob_krong.support.repository.SupportAccessRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AccessPolicyServiceImplTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID SCHOOL_A = UUID.randomUUID();
    private static final UUID SCHOOL_B = UUID.randomUUID();
    private static final UUID BRANCH_1 = UUID.randomUUID();

    @Mock
    private RoleCatalogProvider catalogProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private SupportAccessRepository supportAccessRepository;

    @InjectMocks
    private AccessPolicyServiceImpl accessPolicy;

    @BeforeEach
    void setUp() {
        lenient().when(catalogProvider.get()).thenReturn(SeedRoles.catalog());
    }

    @Test
    void inactiveUserOnlyGetsAnonymousAccess() {
        givenUser(false);
        givenSchool(SCHOOL_A, "approved");

        assertThat(accessPolicy.canAccess(USER, "school.profile:read", SCHOOL_A, null, null))
                .isTrue();
        assertThat(accessPolicy.canAccess(USER, "member:read", SCHOOL_A, null, USER))
                .isFalse();
        verifyNoInteractions(userRoleRepository);
    }

    @Test
    void grantOnClosedBranchIsIgnored() {
        givenUser(true);
        givenSchool(SCHOOL_A, "approved");
        givenGrants(grant(SeedRoles.INSTRUCTOR, SCHOOL_A, BRANCH_1));

        givenBranch(BRANCH_1, SCHOOL_A, "closed");
        assertThat(accessPolicy.canAccess(USER, "media:upload", SCHOOL_A, BRANCH_1, null))
                .isFalse();

        givenBranch(BRANCH_1, SCHOOL_A, "active");
        assertThat(accessPolicy.canAccess(USER, "media:upload", SCHOOL_A, BRANCH_1, null))
                .isTrue();
    }

    @Test
    void supportSessionCountsOnlyForHoldersOfSupportStart() {
        givenUser(true);
        givenSchool(SCHOOL_A, "suspended");

        givenGrants();
        assertThat(accessPolicy.canAccess(USER, "member:read", SCHOOL_A, null, null))
                .isFalse();
        verifyNoInteractions(supportAccessRepository);

        givenGrants(grant(SeedRoles.SUPER_ADMIN, null, null));
        when(supportAccessRepository.findAll(ArgumentMatchers.<Specification<SupportAccessEntity>>any()))
                .thenReturn(List.of(activeSupportSession(SCHOOL_A)));
        assertThat(accessPolicy.canAccess(USER, "member:read", SCHOOL_A, null, null))
                .isTrue();
    }

    @Test
    void branchOfAnotherSchoolIsTreatedAsMissing() {
        givenUser(true);
        givenSchool(SCHOOL_A, "approved");
        givenGrants(grant(SeedRoles.SUPER_ADMIN, null, null));

        givenBranch(BRANCH_1, SCHOOL_B, "active");
        assertThat(accessPolicy.canAccess(USER, "school:review", SCHOOL_A, BRANCH_1, null))
                .isFalse();

        givenBranch(BRANCH_1, SCHOOL_A, "active");
        assertThat(accessPolicy.canAccess(USER, "school:review", SCHOOL_A, BRANCH_1, null))
                .isTrue();
    }

    @Test
    void grantsHeldInSuspendedOrRejectedSchoolsCannotGrant() {
        givenUser(true);
        givenGrants(grant(SeedRoles.SCHOOL_OWNER, SCHOOL_A, null));

        for (String status : List.of("suspended", "rejected")) {
            givenGrantSchools(school(SCHOOL_A, status));
            assertThat(accessPolicy.canGrantRole(USER, SeedRoles.SCHOOL_ADMIN, SCHOOL_A, null))
                    .as("school %s", status)
                    .isFalse();
        }

        givenGrantSchools(school(SCHOOL_A, "draft"));
        assertThat(accessPolicy.canGrantRole(USER, SeedRoles.SCHOOL_ADMIN, SCHOOL_A, null))
                .isTrue();
    }

    @Test
    void inactiveGranterCannotGrant() {
        givenUser(false);

        assertThat(accessPolicy.canGrantRole(USER, SeedRoles.SCHOOL_OWNER, SCHOOL_A, null))
                .isFalse();
        assertThat(accessPolicy.grantableRoleIds(USER, SCHOOL_A, null)).isEmpty();
        verifyNoInteractions(userRoleRepository);
    }

    private void givenUser(boolean active) {
        UserEntity user = new UserEntity();
        user.setId(USER);
        user.setKind("person");
        user.setActive(active);
        when(userRepository.findById(USER)).thenReturn(Optional.of(user));
    }

    private void givenSchool(UUID schoolId, String status) {
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school(schoolId, status)));
    }

    private void givenGrantSchools(SchoolEntity... schools) {
        when(schoolRepository.findAllById(any())).thenReturn(List.of(schools));
    }

    private void givenBranch(UUID branchId, UUID schoolId, String status) {
        BranchEntity branch = new BranchEntity();
        branch.setId(branchId);
        branch.setSchoolId(schoolId);
        branch.setStatus(status);
        lenient().when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        lenient().when(branchRepository.findAllById(any())).thenReturn(List.of(branch));
    }

    private void givenGrants(UserRoleEntity... grants) {
        when(userRoleRepository.findAll(ArgumentMatchers.<Specification<UserRoleEntity>>any()))
                .thenReturn(List.of(grants));
    }

    private static SchoolEntity school(UUID schoolId, String status) {
        SchoolEntity school = new SchoolEntity();
        school.setId(schoolId);
        school.setStatus(status);
        return school;
    }

    private static UserRoleEntity grant(UUID roleId, UUID schoolId, UUID branchId) {
        UserRoleEntity grant = new UserRoleEntity();
        grant.setUserId(USER);
        grant.setRoleId(roleId);
        grant.setSchoolId(schoolId);
        grant.setBranchId(branchId);
        return grant;
    }

    private static SupportAccessEntity activeSupportSession(UUID schoolId) {
        SupportAccessEntity session = new SupportAccessEntity();
        session.setUserId(USER);
        session.setSchoolId(schoolId);
        session.setStartedAt(Instant.now().minusSeconds(60));
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        return session;
    }
}
