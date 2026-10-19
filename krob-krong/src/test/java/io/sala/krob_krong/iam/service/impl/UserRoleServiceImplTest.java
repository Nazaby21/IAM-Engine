package io.sala.krob_krong.iam.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.exceptions.KrobKrongException;
import io.sala.krob_krong.iam.dto.GrantRoleRequest;
import io.sala.krob_krong.iam.entity.RoleEntity;
import io.sala.krob_krong.iam.entity.UserRoleEntity;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.iam.mapper.UserRoleMapper;
import io.sala.krob_krong.iam.repository.RoleRepository;
import io.sala.krob_krong.iam.repository.UserRoleRepository;
import io.sala.krob_krong.iam.security.AccessPolicyService;
import io.sala.krob_krong.iam.security.MfaSessionGuard;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;

class UserRoleServiceImplTest {

    private final UserRoleRepository grants = mock(UserRoleRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AccessPolicyService accessPolicy = mock(AccessPolicyService.class);
    private final MfaSessionGuard mfaSession = mock(MfaSessionGuard.class);
    private final UserRoleServiceImpl service =
            new UserRoleServiceImpl(grants, roles, users, accessPolicy, mock(UserRoleMapper.class), mfaSession);

    private final UUID actorId = UUID.randomUUID();
    private final UUID inviteeId = UUID.randomUUID();
    private final RoleEntity superAdmin = role("super_admin", "platform");
    private final RoleEntity student = role("student", "school");

    @BeforeEach
    void setUp() {
        when(users.existsById(inviteeId)).thenReturn(true);
        when(grants.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void platformInviteNeedsAnMfaVerifiedSession() {
        doThrow(new BusinessException(IAMErrorCode.MFA_REQUIRED))
                .when(mfaSession)
                .require();

        assertError(() -> service.grantPlatformRole(request(superAdmin), actorId), "MFA_REQUIRED");
        verifyNoInteractions(roles, grants);
    }

    @Test
    void platformInviteCreatesAPendingGrantWithoutASchool() {
        when(accessPolicy.canGrantRole(actorId, superAdmin.getId(), null, null)).thenReturn(true);

        service.grantPlatformRole(request(superAdmin), actorId);

        var saved = ArgumentCaptor.forClass(UserRoleEntity.class);
        verify(grants).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(inviteeId);
        assertThat(saved.getValue().getSchoolId()).isNull();
        assertThat(saved.getValue().getBranchId()).isNull();
        assertThat(saved.getValue().getSource()).isEqualTo("invite");
        assertThat(saved.getValue().getGrantedBy()).isEqualTo(actorId);
        assertThat(saved.getValue().getAcceptedAt()).isNull();
    }

    @Test
    void platformInviteFollowsGrantRules() {
        assertError(() -> service.grantPlatformRole(request(superAdmin), actorId), "ROLE_GRANT_NOT_ALLOWED");
        verify(grants, never()).save(any());
    }

    @Test
    void platformInviteRefusesAnInviteeWhoAlreadyHasTheRole() {
        when(accessPolicy.canGrantRole(actorId, superAdmin.getId(), null, null)).thenReturn(true);
        when(grants.exists(any(Specification.class))).thenReturn(true);

        assertError(() -> service.grantPlatformRole(request(superAdmin), actorId), "ROLE_ALREADY_HELD");
    }

    @Test
    void platformInviteRefusesSchoolRolesAndBranches() {
        assertError(() -> service.grantPlatformRole(request(student), actorId), "ROLE_NOT_GRANTABLE_HERE");

        GrantRoleRequest branchLimited = request(superAdmin);
        branchLimited.setBranchId(UUID.randomUUID());
        assertError(() -> service.grantPlatformRole(branchLimited, actorId), "ROLE_NOT_GRANTABLE_HERE");
        verify(grants, never()).save(any());
    }

    @Test
    void schoolInviteRefusesPlatformRoles() {
        assertError(
                () -> service.grantRole(UUID.randomUUID(), request(superAdmin), actorId), "ROLE_NOT_GRANTABLE_HERE");
        verifyNoInteractions(mfaSession);
        verify(grants, never()).save(any());
    }

    private GrantRoleRequest request(RoleEntity role) {
        GrantRoleRequest request = new GrantRoleRequest();
        request.setUserId(inviteeId);
        request.setRoleId(role.getId());
        return request;
    }

    private RoleEntity role(String code, String kind) {
        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        role.setKind(kind);
        when(roles.findById(role.getId())).thenReturn(Optional.of(role));
        return role;
    }

    private static void assertError(ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(KrobKrongException.class, ex -> assertThat(ex.errorCode())
                .isEqualTo(code));
    }
}
