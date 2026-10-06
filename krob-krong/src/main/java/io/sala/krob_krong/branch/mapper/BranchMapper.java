package io.sala.krob_krong.branch.mapper;

import io.sala.krob_krong.branch.dto.*;
import io.sala.krob_krong.branch.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BranchMapper {
    @Mapping(target = "workspaceSlug", ignore = true)
    BranchResponse toResponse(BranchEntity branch);

    WorkspaceResponse toWorkspace(WorkspaceEntity workspace);
}
