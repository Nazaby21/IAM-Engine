package io.sala.krob_krong.school.service.impl;

import io.sala.krob_krong.account.entity.UserEntity;
import io.sala.krob_krong.account.repository.UserRepository;
import io.sala.krob_krong.branch.dto.BranchResponse;
import io.sala.krob_krong.branch.entity.BranchEntity;
import io.sala.krob_krong.branch.mapper.BranchMapper;
import io.sala.krob_krong.branch.repository.*;
import io.sala.krob_krong.branch.specification.*;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.iam.repository.UserRoleRepository;
import io.sala.krob_krong.iam.security.AccessPolicyService;
import io.sala.krob_krong.iam.security.Principals;
import io.sala.krob_krong.iam.specification.UserRoleSpecification;
import io.sala.krob_krong.media.repository.MediaAssetRepository;
import io.sala.krob_krong.school.dto.*;
import io.sala.krob_krong.school.entity.*;
import io.sala.krob_krong.school.mapper.SchoolMapper;
import io.sala.krob_krong.school.repository.*;
import io.sala.krob_krong.school.service.*;
import io.sala.krob_krong.school.specification.*;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchoolServiceImpl implements SchoolService {
    private final SchoolRepository schools;
    private final SchoolReviewRepository reviews;
    private final UserRepository users;
    private final UserRoleRepository grants;
    private final BranchRepository branches;
    private final WorkspaceRepository workspaces;
    private final MediaAssetRepository media;
    private final SchoolAccess access;
    private final AccessPolicyService policy;
    private final SchoolMapper mapper;
    private final BranchMapper branchMapper;
    private final EntityManager em;

    @Override
    @Transactional
    public SchoolResponse register(SchoolProfileRequest request) {
        UUID actor = SchoolAccess.actor();
        UserEntity user = users.findByIdForUpdate(actor).orElseThrow(() -> new BusinessException(IAMErrorCode.USER_NOT_FOUND));
        if (!user.isPerson() || !user.isActive() || !user.isVerified())
            throw new BusinessException(IAMErrorCode.EMAIL_VERIFICATION_REQUIRED);
        SchoolEntity school = new SchoolEntity();
        school.setCreatedBy(actor);
        school.setStatusChangedAt(Instant.now());
        applyProfile(school, request);
        schools.saveAndFlush(school);
        em.refresh(school); // Database grants school_owner atomically via school_onboard.
        BranchEntity branch = new BranchEntity();
        branch.setSchoolId(school.getId());
        branch.setName("Main branch");
        branch.setDefaultBranch(true);
        branch.setCreatedBy(actor);
        branches.saveAndFlush(branch);
        return mapper.toResponse(school);
    }

    @Override
    public SchoolResponse get(UUID id) {
        return mapper.toResponse(access.read(id, "school.profile:read"));
    }

    @Override
    public PageResponse<SchoolSummaryResponse> mine(PageRequest page) {
        // Owners can see status/rejection reasons even when suspended, without access to school data.
        return PageResponse.from(schools.findAll(
                        SchoolSpecification.byCreatedBy(SchoolAccess.actor()),
                        page.toSpring(Sort.by(Sort.Direction.DESC, SchoolEntity.Fields.createdAt)))
                .map(mapper::toSummary));
    }

    @Override
    @Transactional
    public SchoolResponse update(UUID id, SchoolProfileRequest request) {
        SchoolEntity school = access.lock(id, "school.profile:edit");
        SchoolAccess.editable(school);
        if ("approved".equals(school.getStatus())
                && (!Objects.equals(school.getDisplayName(), SchoolAccess.clean(request.getDisplayName()))
                        || !Objects.equals(school.getLegalName(), SchoolAccess.clean(request.getLegalName()))
                        || !Objects.equals(
                                school.getRegistrationNo(), SchoolAccess.clean(request.getRegistrationNo())))) {
            throw new BusinessException(
                    IAMErrorCode.SCHOOL_PROFILE_LOCKED, "Approved identity fields cannot change; contact support");
        }
        applyProfile(school, request);
        return save(school);
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingResponse onboarding(UUID id) {
        SchoolEntity school = access.read(id, "school.profile:edit");
        BranchEntity branch =
                branches.findOne(BranchSpecification.defaultOfSchool(id)).orElse(null);
        BranchResponse response = branch == null ? null : branchMapper.toResponse(branch);
        if (response != null)
            response.setWorkspaceSlug(workspaces
                    .findOne(WorkspaceSpecification.currentForBranch(branch.getId()))
                    .map(w -> w.getSlug())
                    .orElse(null));
        var missing = missing(school, branch);
        return OnboardingResponse.builder()
                .school(mapper.toResponse(school))
                .defaultBranch(response)
                .missingFields(missing)
                .canSubmit(Set.of("draft", "changes_requested").contains(school.getStatus()) && missing.isEmpty())
                .build();
    }

    @Override
    @Transactional
    public SchoolResponse submit(UUID id) {
        SchoolEntity school = access.lock(id, "school:submit");
        if (!Set.of("draft", "changes_requested").contains(school.getStatus()))
            throw new BusinessException(IAMErrorCode.SCHOOL_STATUS_INVALID);
        var missing = missing(
                school,
                branches.findOne(BranchSpecification.defaultOfSchool(id)).orElse(null));
        if (!missing.isEmpty())
            throw new BusinessException(IAMErrorCode.SCHOOL_PROFILE_INCOMPLETE).withContext("missing_fields", missing);
        school.setStatus("pending_review");
        school.setStatusReason(null);
        return save(school); // Trigger stores one immutable review snapshot.
    }

    @Override
    public PageResponse<SchoolResponse> directory(String status, PageRequest page) {
        access.check(null, "school.directory:read");
        requireMfa();
        if (status != null
                && !Set.of("draft", "pending_review", "changes_requested", "approved", "rejected", "suspended")
                        .contains(status)) throw new BusinessException(IAMErrorCode.SCHOOL_STATUS_INVALID);
        return PageResponse.from(schools.findAll(
                        status == null ? (root, query, cb) -> cb.conjunction() : SchoolSpecification.byStatus(status),
                        page.toSpring(Sort.by(Sort.Direction.DESC, SchoolEntity.Fields.statusChangedAt)))
                .map(mapper::toResponse));
    }

    @Override
    public List<SchoolReviewResponse> reviews(UUID id) {
        access.read(id, "school.profile:read");
        UUID actor = SchoolAccess.actor();
        if (!policy.canAccess(actor, "school.profile:edit", id, null, null)
                && !policy.canAccess(actor, "school:review", id, null, null))
            throw new BusinessException(io.sala.krob_krong.common.errors.CommonErrorCode.FORBIDDEN);
        return reviews
                .findAll(
                        SchoolReviewSpecification.bySchoolId(id),
                        Sort.by(Sort.Direction.DESC, SchoolReviewEntity.Fields.submittedAt))
                .stream()
                .map(mapper::toReview)
                .toList();
    }

    @Override
    @Transactional
    public SchoolResponse review(UUID id, ReviewDecisionRequest request) {
        SchoolEntity school = access.lock(id, "school:review");
        requireMfa();
        if (!"pending_review".equals(school.getStatus()))
            throw new BusinessException(IAMErrorCode.SCHOOL_STATUS_INVALID);
        var open = reviews.findOne(SchoolReviewSpecification.pendingForSchool(id))
                .orElseThrow(() -> new BusinessException(IAMErrorCode.REVIEW_STALE));
        if (!open.getId().equals(request.getReviewId())) throw new BusinessException(IAMErrorCode.REVIEW_STALE);
        UUID actor = SchoolAccess.actor();
        if (actor.equals(school.getCreatedBy())
                || grants.exists(UserRoleSpecification.byUserId(actor).and(UserRoleSpecification.bySchoolId(id))))
            throw new BusinessException(IAMErrorCode.REVIEW_CONFLICT_OF_INTEREST);
        if (!Set.of("approved", "changes_requested", "rejected").contains(request.getDecision()))
            throw new BusinessException(IAMErrorCode.SCHOOL_STATUS_INVALID);
        String reason = SchoolAccess.clean(request.getReason());
        if (!"approved".equals(request.getDecision()) && reason == null)
            throw new BusinessException(
                    IAMErrorCode.SCHOOL_STATUS_INVALID, "A reason is required for changes or rejection");
        school.setStatus(request.getDecision());
        school.setStatusReason(reason);
        return save(school);
    }

    @Override
    @Transactional
    public SchoolResponse suspend(UUID id, String reason) {
        SchoolEntity school = access.lock(id, "school:suspend");
        requireMfa();
        if (!"approved".equals(school.getStatus()) || SchoolAccess.clean(reason) == null)
            throw new BusinessException(IAMErrorCode.SCHOOL_STATUS_INVALID);
        school.setStatus("suspended");
        school.setStatusReason(SchoolAccess.clean(reason));
        return save(school);
    }

    @Override
    @Transactional
    public SchoolResponse reinstate(UUID id) {
        SchoolEntity school = access.lock(id, "school:suspend");
        requireMfa();
        if (!"suspended".equals(school.getStatus())) throw new BusinessException(IAMErrorCode.SCHOOL_STATUS_INVALID);
        school.setStatus("approved");
        school.setStatusReason(null);
        return save(school);
    }

    private void requireMfa() {
        var principal = Principals.current().orElseThrow(() -> new BusinessException(IAMErrorCode.MFA_REQUIRED));
        if (!principal.mfaVerified()
                || users.findById(principal.id())
                        .filter(u -> u.getMfaEnabledAt() != null)
                        .isEmpty()) throw new BusinessException(IAMErrorCode.MFA_REQUIRED);
    }

    private List<String> missing(SchoolEntity school, BranchEntity branch) {
        List<String> missing = new ArrayList<>();
        if (readyImage(school.getId(), school.getLogoAssetId(), "school_logo")) missing.add("logo_asset_id");
        if (readyImage(school.getId(), school.getCoverAssetId(), "school_cover")) missing.add("cover_asset_id");
        if (branch == null) missing.add("default_branch");
        else {
            if (SchoolAccess.clean(branch.getAddressLine()) == null) missing.add("default_branch.address_line");
            if (SchoolAccess.clean(branch.getProvince()) == null) missing.add("default_branch.province");
            if (branch.getLatitude() == null || branch.getLongitude() == null) missing.add("default_branch.location");
            if (!workspaces.exists(WorkspaceSpecification.currentForBranch(branch.getId())))
                missing.add("workspace_slug");
        }
        return missing;
    }

    private boolean readyImage(UUID schoolId, UUID id, String purpose) {
        return id == null
                || media.findById(id)
                .filter(a -> a.isReady() && schoolId.equals(a.getSchoolId()) && purpose.equals(a.getPurpose()))
                .isEmpty();
    }

    private SchoolResponse save(SchoolEntity school) {
        schools.saveAndFlush(school);
        em.refresh(school);
        return mapper.toResponse(school);
    }

    private static void applyProfile(SchoolEntity school, SchoolProfileRequest r) {
        school.setDisplayName(SchoolAccess.clean(r.getDisplayName()));
        school.setLegalName(SchoolAccess.clean(r.getLegalName()));
        school.setRegistrationNo(SchoolAccess.clean(r.getRegistrationNo()));
        school.setDescription(SchoolAccess.clean(r.getDescription()));
        school.setContactEmail(SchoolAccess.clean(r.getContactEmail()));
        school.setContactPhone(SchoolAccess.clean(r.getContactPhone()));
    }
}
