package io.sala.krob_krong.iam.controller;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.iam.dto.GrantRoleRequest;
import io.sala.krob_krong.iam.dto.MemberResponse;
import io.sala.krob_krong.iam.dto.RevokeRoleRequest;
import io.sala.krob_krong.iam.dto.UserRoleResponse;
import io.sala.krob_krong.iam.security.AuthenticatedUser;
import io.sala.krob_krong.iam.security.RequirePermission;
import io.sala.krob_krong.iam.service.UserRoleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserRoleController {

    private final UserRoleService userRoleService;

    // Authorized in the service: the grant rule depends on the role and branch in the body.
    @PostMapping("/schools/{school_id}/members/invite")
    public ApiResponse<UserRoleResponse> inviteMember(
            @PathVariable("school_id") UUID schoolId,
            @Valid @RequestBody GrantRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.create(userRoleService.grantRole(schoolId, request, user.id()));
    }

    @GetMapping("/schools/{school_id}/members")
    @RequirePermission(value = "member:read", school = "#schoolId")
    public ApiResponse<PageResponse<MemberResponse>> listMembers(
            @PathVariable("school_id") UUID schoolId, PageRequest pageRequest) {
        return ApiResponse.paginated(userRoleService.listSchoolMembers(schoolId, pageRequest));
    }

    @GetMapping("/schools/{school_id}/members/{user_id}/roles")
    @RequirePermission(value = "member:read", school = "#schoolId", target = "#userId")
    public ApiResponse<List<UserRoleResponse>> listUserRoles(
            @PathVariable("school_id") UUID schoolId, @PathVariable("user_id") UUID userId) {
        return ApiResponse.success(userRoleService.listUserRoles(userId, schoolId));
    }

    @PostMapping("/me/invitations/{user_role_id}/accept")
    public ApiResponse<UserRoleResponse> acceptInvitation(
            @PathVariable("user_role_id") UUID userRoleId, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(userRoleService.acceptInvitation(userRoleId, user.id()));
    }

    @GetMapping("/me/invitations")
    public ApiResponse<List<UserRoleResponse>> myInvitations(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(userRoleService.myInvitations(user.id()));
    }

    // Authorized in the service: holders may drop their own grant, otherwise the grant rule applies.
    @DeleteMapping("/user-roles/{user_role_id}")
    public ApiResponse<UserRoleResponse> revokeRole(
            @PathVariable("user_role_id") UUID userRoleId,
            @RequestBody(required = false) RevokeRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        String reason = request != null ? request.getReason() : null;
        return ApiResponse.success(userRoleService.revokeGrant(userRoleId, reason, user.id()));
    }
}
