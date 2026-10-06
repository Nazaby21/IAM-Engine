package io.sala.krob_krong.iam.controller;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.iam.dto.RoleDetailResponse;
import io.sala.krob_krong.iam.dto.RoleResponse;
import io.sala.krob_krong.iam.security.AuthenticatedUser;
import io.sala.krob_krong.iam.service.RoleService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ApiResponse<PageResponse<RoleResponse>> listRoles(
            @RequestParam(required = false) String kind, PageRequest pageRequest) {
        return ApiResponse.paginated(roleService.listRoles(kind, pageRequest));
    }

    @GetMapping("/{role_id}")
    public ApiResponse<RoleDetailResponse> getRole(@PathVariable("role_id") UUID roleId) {
        return ApiResponse.success(roleService.getRole(roleId));
    }

    @GetMapping("/grantable")
    public ApiResponse<List<RoleResponse>> getGrantableRoles(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(name = "school_id", required = false) UUID schoolId,
            @RequestParam(name = "branch_id", required = false) UUID branchId) {
        return ApiResponse.success(roleService.getGrantableRoles(user.id(), schoolId, branchId));
    }
}
