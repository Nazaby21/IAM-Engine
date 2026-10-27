package io.sala.krob_krong.subject.service.Impl;

import io.sala.krob_krong.branch.repository.BranchRepository;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.subject.dto.SubjectCreateRequest;
import io.sala.krob_krong.subject.dto.SubjectResponse;
import io.sala.krob_krong.subject.dto.SubjectUpdateRequest;
import io.sala.krob_krong.subject.entity.SubjectEntity;
import io.sala.krob_krong.subject.mapper.SubjectMapper;
import io.sala.krob_krong.subject.repository.SubjectRepository;
import io.sala.krob_krong.subject.service.SubjectService;
import io.sala.krob_krong.subject.specification.SubjectSpecification;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;
    private final BranchRepository branchRepository;
    private static final String STATUS_ACTIVE = "ACTIVE";

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubjectResponse> getAllSubjectInBranch(UUID branchId, PageRequest pageRequest) {
        Specification<SubjectEntity> spec = SubjectSpecification.byBranchId(branchId)
                .and(SubjectSpecification.byStatus(STATUS_ACTIVE))
                .and(SubjectSpecification.notDeleted());

        return PageResponse.from(subjectRepository
                .findAll(spec, pageRequest.toSpring(Sort.by(Sort.Direction.DESC, SubjectEntity.Fields.name)))
                .map(subjectMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getById(UUID branchId, UUID subjectId) {
        return subjectMapper.toResponse(findActive(branchId, subjectId));
    }

    @Override
    public SubjectResponse create(UUID branchId, SubjectCreateRequest request) {
        if (!branchRepository.existsById(branchId)) {
            throw new BusinessException(IAMErrorCode.BRANCH_NOT_FOUND);
        }
        if (subjectRepository.existsByBranchIdAndNameIgnoreCaseAndDeletedAtIsNull(branchId, request.getName())) {
            throw new BusinessException(IAMErrorCode.SUBJECT_ALREADY_EXISTS);
        }

        var subject = subjectMapper.toEntity(request);
        subject.setBranchId(branchId);
        subject.setCreatedBy(UUID.fromString("dd58c768-a6dd-4f59-9ab9-63ff55898834"));

        return subjectMapper.toResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public SubjectResponse update(UUID branchId, UUID subjectId, SubjectUpdateRequest request) {
        SubjectEntity subject = findActive(branchId, subjectId);

        if (request.getName() != null
                && subjectRepository.existsDuplicateName(branchId, request.getName(), subjectId)) {
            throw new BusinessException(IAMErrorCode.SUBJECT_ALREADY_EXISTS);
        }

        subjectMapper.updateEntity(request, subject);
        subject.setUpdatedBy(UUID.fromString("dd58c768-a6dd-4f59-9ab9-63ff55898834"));
        subject.setUpdatedAt(OffsetDateTime.now().toInstant());

        return subjectMapper.toResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public void delete(UUID branchId, UUID subjectId) {
        SubjectEntity subject = findActive(branchId, subjectId);

        subject.setDeletedAt(OffsetDateTime.now());
        subject.setDeletedBy(UUID.fromString("dd58c768-a6dd-4f59-9ab9-63ff55898834"));
        subjectRepository.save(subject);
    }

    private SubjectEntity findActive(UUID branchId, UUID subjectId) {
        return subjectRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(subjectId, branchId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.SUBJECT_NOT_FOUND));
    }
}
