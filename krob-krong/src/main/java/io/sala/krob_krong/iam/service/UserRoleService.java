package io.sala.krob_krong.iam.service;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.iam.dto.GrantRoleRequest;
import io.sala.krob_krong.iam.dto.MemberResponse;
import io.sala.krob_krong.iam.dto.UserRoleResponse;
import java.util.List;
import java.util.UUID;

public interface UserRoleService {

    UserRoleResponse grantRole(UUID schoolId, GrantRoleRequest request, UUID actorId);

    UserRoleResponse acceptInvitation(UUID userRoleId, UUID actorId);

    UserRoleResponse revokeGrant(UUID userRoleId, String reason, UUID actorId);

    List<UserRoleResponse> listUserRoles(UUID userId, UUID schoolId);

    PageResponse<MemberResponse> listSchoolMembers(UUID schoolId, PageRequest pageRequest);

    List<UserRoleResponse> myInvitations(UUID userId);
}
