package io.sala.krob_krong.iam.controller;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.iam.dto.PermissionResponse;
import io.sala.krob_krong.iam.security.AuthenticatedUser;
import io.sala.krob_krong.iam.service.PermissionService;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ApiResponse<PageResponse<PermissionResponse>> listPermissions(
            @RequestParam(required = false) String search, PageRequest pageRequest) {
        return ApiResponse.paginated(permissionService.listPermissions(search, pageRequest));
    }

    @GetMapping("/{code}")
    public ApiResponse<PermissionResponse> getPermission(@PathVariable String code) {
        return ApiResponse.success(permissionService.getPermission(code));
    }

    @GetMapping("/effective")
    public ApiResponse<Set<String>> effectivePermissions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(name = "school_id", required = false) UUID schoolId,
            @RequestParam(name = "branch_id", required = false) UUID branchId) {
        return ApiResponse.success(permissionService.effectivePermissions(user.id(), schoolId, branchId));
    }
}
