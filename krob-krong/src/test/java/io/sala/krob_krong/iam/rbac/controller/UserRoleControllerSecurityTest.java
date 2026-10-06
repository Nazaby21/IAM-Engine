package io.sala.krob_krong.iam.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.sala.krob_krong.audit.service.AccessLogService;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.iam.dto.GrantRoleRequest;
import io.sala.krob_krong.iam.dto.MemberResponse;
import io.sala.krob_krong.iam.dto.RoleDetailResponse;
import io.sala.krob_krong.iam.dto.RolePermissionResponse;
import io.sala.krob_krong.iam.dto.UserRoleResponse;
import io.sala.krob_krong.iam.security.AccessPolicyService;
import io.sala.krob_krong.iam.security.AuthenticatedUser;
import io.sala.krob_krong.iam.security.RbacAuthorizer;
import io.sala.krob_krong.iam.security.config.SecurityConfig;
import io.sala.krob_krong.iam.security.jwt.UserAuthenticationToken;
import io.sala.krob_krong.iam.security.jwt.UserJwtAuthenticationConverter;
import io.sala.krob_krong.iam.security.web.AccessDeniedExceptionMapper;
import io.sala.krob_krong.iam.security.web.AuthenticationExceptionMapper;
import io.sala.krob_krong.iam.security.web.RestAccessDeniedHandler;
import io.sala.krob_krong.iam.security.web.RestAuthenticationEntryPoint;
import io.sala.krob_krong.iam.service.PermissionService;
import io.sala.krob_krong.iam.service.RoleService;
import io.sala.krob_krong.iam.service.UserRoleService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({UserRoleController.class, RoleController.class, PermissionController.class})
@Import({
    SecurityConfig.class,
    RbacAuthorizer.class,
    UserJwtAuthenticationConverter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    AuthenticationExceptionMapper.class,
    AccessDeniedExceptionMapper.class
})
class UserRoleControllerSecurityTest {

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private PermissionService permissionService;

    private static final String MEMBER_READ = "member:read";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserRoleService userRoleService;

    @MockitoBean
    private AccessPolicyService accessPolicy;

    @MockitoBean
    private AccessLogService accessLog;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void requestWithoutTokenGets401InApiEnvelope() throws Exception {
        mvc.perform(get("/api/schools/{schoolId}/members", schoolId))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.category").value("AUTHENTICATION"))
                .andExpect(jsonPath("$.request_id").isNotEmpty())
                .andExpect(jsonPath("$.requestId").doesNotExist());

        verifyNoInteractions(userRoleService, accessPolicy);
    }

    @Test
    void invalidTokenGets401WithInvalidTokenChallenge() throws Exception {
        when(jwtDecoder.decode("forged")).thenThrow(new BadJwtException("Invalid signature"));

        mvc.perform(get("/api/schools/{schoolId}/members", schoolId).header(HttpHeaders.AUTHORIZATION, "Bearer forged"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\""))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(userRoleService);
    }

    @Test
    void validBearerTokenReachesPermissionCheckAsItsSubject() throws Exception {
        Jwt jwt = Jwt.withTokenValue("valid")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("sid", UUID.randomUUID().toString())
                .claim("kind", AuthenticatedUser.KIND_PERSON)
                .build();
        when(jwtDecoder.decode("valid")).thenReturn(jwt);
        when(accessPolicy.canAccess(userId, MEMBER_READ, schoolId, null, null)).thenReturn(true);
        when(accessPolicy.isSensitive(MEMBER_READ)).thenReturn(true);
        when(userRoleService.listSchoolMembers(eq(schoolId), any())).thenReturn(PageResponse.from(Page.empty()));

        mvc.perform(get("/api/schools/{schoolId}/members", schoolId).header(HttpHeaders.AUTHORIZATION, "Bearer valid"))
                .andExpect(status().isOk());

        verify(accessLog).record(userId, MEMBER_READ, schoolId, null, null, true);
    }

    @Test
    void missingPermissionGets403AndIsAudited() throws Exception {
        when(accessPolicy.canAccess(userId, MEMBER_READ, schoolId, null, null)).thenReturn(false);

        mvc.perform(get("/api/schools/{schoolId}/members", schoolId).with(authentication(authenticated(userId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.category").value("AUTHORIZATION"));

        verify(accessLog).record(userId, MEMBER_READ, schoolId, null, null, false);
        verifyNoInteractions(userRoleService);
    }

    @Test
    void memberRoleLookupPassesTargetUserSoOwnScopeApplies() throws Exception {
        when(accessPolicy.canAccess(userId, MEMBER_READ, schoolId, null, userId))
                .thenReturn(true);
        when(userRoleService.listUserRoles(userId, schoolId)).thenReturn(List.of());

        mvc.perform(get("/api/schools/{schoolId}/members/{userId}/roles", schoolId, userId)
                        .with(authentication(authenticated(userId))))
                .andExpect(status().isOk());
    }

    @Test
    void inviteActsAsTheAuthenticatedUserNotARequestParameter() throws Exception {
        UUID invitee = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        mvc.perform(post("/api/schools/{schoolId}/members/invite", schoolId)
                        .with(authentication(authenticated(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":\"" + invitee + "\",\"role_id\":\"" + roleId + "\"}"))
                .andExpect(status().isCreated());

        verify(userRoleService).grantRole(eq(schoolId), any(GrantRoleRequest.class), eq(userId));
    }

    @Test
    void snakeCaseQueryParametersReachRoleAndPermissionServices() throws Exception {
        UUID branchId = UUID.randomUUID();
        when(roleService.getGrantableRoles(userId, schoolId, branchId)).thenReturn(List.of());
        when(permissionService.effectivePermissions(userId, schoolId, branchId)).thenReturn(Set.of("member:read"));
        mvc.perform(get("/api/roles/grantable")
                        .with(authentication(authenticated(userId)))
                        .param("school_id", schoolId.toString())
                        .param("branch_id", branchId.toString()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/permissions/effective")
                        .with(authentication(authenticated(userId)))
                        .param("school_id", schoolId.toString())
                        .param("branch_id", branchId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("member:read"));
        verify(roleService).getGrantableRoles(userId, schoolId, branchId);
        verify(permissionService).effectivePermissions(userId, schoolId, branchId);
    }

    @Test
    void nestedRoleAndPaginationResponsesUseSnakeCase() throws Exception {
        when(accessPolicy.canAccess(userId, MEMBER_READ, schoolId, null, null)).thenReturn(true);
        var grant = UserRoleResponse.builder()
                .userId(userId)
                .schoolId(schoolId)
                .roleCode("student")
                .validFrom(Instant.parse("2026-01-01T00:00:00Z"))
                .status("active")
                .build();
        var member = MemberResponse.builder()
                .userId(userId)
                .displayName("Normal Person")
                .roles(List.of(grant))
                .build();
        when(userRoleService.listSchoolMembers(eq(schoolId), any()))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of(member))));
        mvc.perform(get("/api/schools/{school_id}/members", schoolId).with(authentication(authenticated(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request_id").isNotEmpty())
                .andExpect(jsonPath("$.requestId").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].user_id").value(userId.toString()))
                .andExpect(jsonPath("$.data.content[0].display_name").value("Normal Person"))
                .andExpect(jsonPath("$.data.content[0].roles[0].school_id").value(schoolId.toString()))
                .andExpect(jsonPath("$.data.content[0].roles[0].role_code").value("student"))
                .andExpect(jsonPath("$.data.content[0].roles[0].valid_from").isNotEmpty())
                .andExpect(jsonPath("$.data.meta.total_elements").value(1))
                .andExpect(jsonPath("$.data.meta.total_pages").value(1))
                .andExpect(jsonPath("$.data.meta.has_next").value(false))
                .andExpect(jsonPath("$.data.meta.totalElements").doesNotExist());
    }

    @Test
    void roleDetailsSerializeBooleanAndNestedPermissionNames() throws Exception {
        UUID roleId = UUID.randomUUID();
        var role = RoleDetailResponse.builder()
                .id(roleId)
                .allowsBranch(true)
                .preApproval(false)
                .grantableRoleIds(List.of(roleId))
                .permissions(List.of(RolePermissionResponse.builder()
                        .permissionCode("member:read")
                        .scope("tenant")
                        .build()))
                .build();
        when(roleService.getRole(roleId)).thenReturn(role);
        mvc.perform(get("/api/roles/{role_id}", roleId).with(authentication(authenticated(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allows_branch").value(true))
                .andExpect(jsonPath("$.data.pre_approval").value(false))
                .andExpect(jsonPath("$.data.grantable_role_ids[0]").value(roleId.toString()))
                .andExpect(jsonPath("$.data.permissions[0].permission_code").value("member:read"));
    }

    @Test
    void invitationBindsAllSnakeCaseFieldsAndNamesValidationErrorsConsistently() throws Exception {
        UUID roleId = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Instant validTo = Instant.parse("2027-01-01T00:00:00Z");
        mvc.perform(post("/api/schools/{school_id}/members/invite", schoolId)
                        .with(authentication(authenticated(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"user_id":"%s","role_id":"%s","branch_id":"%s","valid_to":"%s"}
                    """.formatted(invitee, roleId, branchId, validTo)))
                .andExpect(status().isCreated());
        var request = ArgumentCaptor.forClass(GrantRoleRequest.class);
        verify(userRoleService).grantRole(eq(schoolId), request.capture(), eq(userId));
        org.assertj.core.api.Assertions.assertThat(request.getValue().getUserId())
                .isEqualTo(invitee);
        org.assertj.core.api.Assertions.assertThat(request.getValue().getRoleId())
                .isEqualTo(roleId);
        org.assertj.core.api.Assertions.assertThat(request.getValue().getBranchId())
                .isEqualTo(branchId);
        org.assertj.core.api.Assertions.assertThat(request.getValue().getValidTo())
                .isEqualTo(validTo);
        mvc.perform(post("/api/schools/{school_id}/members/invite", schoolId)
                        .with(authentication(authenticated(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.fields.user_id").exists())
                .andExpect(jsonPath("$.error.details.fields.role_id").exists())
                .andExpect(jsonPath("$.error.details.fields.userId").doesNotExist());
    }

    private static UserAuthenticationToken authenticated(UUID id) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(id.toString())
                .build();
        AuthenticatedUser user =
                new AuthenticatedUser(id, UUID.randomUUID(), AuthenticatedUser.KIND_PERSON, null, null, false);
        return new UserAuthenticationToken(jwt, user, List.of());
    }
}
