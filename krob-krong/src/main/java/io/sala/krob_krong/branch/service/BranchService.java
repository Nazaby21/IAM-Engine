package io.sala.krob_krong.branch.service;

import io.sala.krob_krong.branch.dto.*;
import java.util.*;

public interface BranchService {
    List<BranchResponse> list(UUID schoolId);

    BranchResponse updateDefault(UUID schoolId, BranchProfileRequest request);

    BranchResponse add(UUID schoolId, BranchProfileRequest request);

    BranchResponse update(UUID schoolId, UUID branchId, BranchProfileRequest request);

    BranchResponse open(UUID schoolId, UUID branchId);

    WorkspaceResponse claimWorkspace(UUID schoolId, UUID branchId, String slug);

    WorkspaceResolution resolve(String slug);
}
