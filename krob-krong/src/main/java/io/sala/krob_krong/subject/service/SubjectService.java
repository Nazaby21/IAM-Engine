package io.sala.krob_krong.subject.service;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.subject.dto.SubjectCreateRequest;
import io.sala.krob_krong.subject.dto.SubjectResponse;
import io.sala.krob_krong.subject.dto.SubjectUpdateRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public interface SubjectService {
    PageResponse<SubjectResponse> getAllSubjectInBranch(UUID branchId, PageRequest pageRequest);

    SubjectResponse getById(UUID branchId, UUID subjectId);

    SubjectResponse create(UUID branchId, SubjectCreateRequest request);

    SubjectResponse update(UUID branchId, UUID subjectId, SubjectUpdateRequest request);

    void delete(UUID branchId, UUID subjectId);
}
