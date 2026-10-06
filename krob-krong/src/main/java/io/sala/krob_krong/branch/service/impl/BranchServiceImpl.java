package io.sala.krob_krong.branch.service.impl;

import io.sala.krob_krong.branch.dto.*;
import io.sala.krob_krong.branch.entity.*;
import io.sala.krob_krong.branch.mapper.BranchMapper;
import io.sala.krob_krong.branch.repository.*;
import io.sala.krob_krong.branch.service.BranchService;
import io.sala.krob_krong.branch.specification.*;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.iam.security.AccessPolicyService;
import io.sala.krob_krong.school.service.SchoolAccess;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {
    private final BranchRepository branches;
    private final WorkspaceRepository workspaces;
    private final ReservedSlugRepository reserved;
    private final SchoolAccess access;
    private final AccessPolicyService policy;
    private final BranchMapper mapper;
    private final EntityManager em;
    private final JdbcClient jdbc;

    @Override
    public List<BranchResponse> list(UUID schoolId) {
        access.read(schoolId, "branch:read");
        return branches.findAll(BranchSpecification.bySchoolId(schoolId)).stream()
                .filter(b -> policy.canAccess(
                        io.sala.krob_krong.iam.security.Principals.currentUserId()
                                .orElse(null),
                        "branch:read",
                        schoolId,
                        b.getId(),
                        null))
                .map(this::response)
                .toList();
    }

    @Override
    @Transactional
    public BranchResponse updateDefault(UUID schoolId, BranchProfileRequest request) {
        var school = access.lock(schoolId, "branch:edit");
        SchoolAccess.editable(school);
        var branch = branches.findOne(BranchSpecification.defaultOfSchool(schoolId))
                .orElseThrow(() -> new BusinessException(IAMErrorCode.BRANCH_NOT_FOUND));
        apply(branch, request);
        return save(branch);
    }

    @Override
    @Transactional
    public BranchResponse add(UUID schoolId, BranchProfileRequest request) {
        var school = access.lock(schoolId, "branch:create");
        if (!"approved".equals(school.getStatus()))
            throw new BusinessException(
                    IAMErrorCode.SCHOOL_STATUS_INVALID, "Additional branches are created after school approval");
        var branch = new BranchEntity();
        branch.setSchoolId(schoolId);
        branch.setCreatedBy(SchoolAccess.actor());
        apply(branch, request);
        return save(branch);
    }

    @Override
    @Transactional
    public BranchResponse update(UUID schoolId, UUID branchId, BranchProfileRequest request) {
        var school = access.lock(schoolId, "branch:read");
        SchoolAccess.editable(school);
        requireBranchPermission(schoolId, branchId, "branch:edit");
        var branch = branch(schoolId, branchId);
        apply(branch, request);
        return save(branch);
    }

    @Override
    @Transactional
    public BranchResponse open(UUID schoolId, UUID branchId) {
        var school = access.lock(schoolId, "branch:read");
        requireBranchPermission(schoolId, branchId, "branch:open");
        if (!"approved".equals(school.getStatus())) throw new BusinessException(IAMErrorCode.SCHOOL_STATUS_INVALID);
        var branch = branch(schoolId, branchId);
        if (SchoolAccess.clean(branch.getAddressLine()) == null
                || SchoolAccess.clean(branch.getProvince()) == null
                || branch.getLatitude() == null
                || branch.getLongitude() == null
                || !workspaces.exists(WorkspaceSpecification.currentForBranch(branchId)))
            throw new BusinessException(IAMErrorCode.BRANCH_NOT_READY);
        branch.setStatus("active");
        return save(branch);
    }

    @Override
    @Transactional
    public WorkspaceResponse claimWorkspace(UUID schoolId, UUID branchId, String slug) {
        var school = access.lock(schoolId, "branch:read");
        SchoolAccess.editable(school);
        branch(schoolId, branchId);
        requireBranchPermission(schoolId, branchId, "workspace:edit");
        if (slug == null || !slug.matches("[a-z0-9][a-z0-9-]{1,61}[a-z0-9]") || slug.contains("--"))
            throw new BusinessException(
                    IAMErrorCode.WORKSPACE_SLUG_RESERVED, "Use 3–63 lowercase letters, digits and single hyphens");
        if (reserved.existsById(slug)) throw new BusinessException(IAMErrorCode.WORKSPACE_SLUG_RESERVED);
        var current = workspaces.findOne(WorkspaceSpecification.currentForBranch(branchId));
        if (current.isPresent() && slug.equals(current.get().getSlug())) return mapper.toWorkspace(current.get());
        if (workspaces.exists(WorkspaceSpecification.bySlug(slug)))
            throw new BusinessException(IAMErrorCode.WORKSPACE_SLUG_TAKEN);
        current.ifPresent(w -> {
            w.setRetiredAt(Instant.now());
            workspaces.saveAndFlush(w);
        });
        var workspace = new WorkspaceEntity();
        workspace.setSchoolId(schoolId);
        workspace.setBranchId(branchId);
        workspace.setSlug(slug);
        workspace.setCreatedBy(SchoolAccess.actor());
        workspaces.saveAndFlush(workspace);
        em.refresh(workspace);
        return mapper.toWorkspace(workspace);
    }

    @Override
    public WorkspaceResolution resolve(String slug) {
        return jdbc.sql("SELECT school_id, branch_id, canonical_slug FROM resolve_workspace(:slug)")
                .param("slug", slug)
                .query((rs, n) -> WorkspaceResolution.builder()
                        .schoolId(rs.getObject("school_id", UUID.class))
                        .branchId(rs.getObject("branch_id", UUID.class))
                        .canonicalSlug(rs.getString("canonical_slug"))
                        .build())
                .optional()
                .orElseThrow(() -> new BusinessException(IAMErrorCode.WORKSPACE_NOT_FOUND));
    }

    private void requireBranchPermission(UUID schoolId, UUID branchId, String permission) {
        if (!policy.canAccess(SchoolAccess.actor(), permission, schoolId, branchId, null))
            throw new BusinessException(io.sala.krob_krong.common.errors.CommonErrorCode.FORBIDDEN);
    }

    private BranchEntity branch(UUID schoolId, UUID branchId) {
        return branches.findById(branchId)
                .filter(b -> schoolId.equals(b.getSchoolId()))
                .orElseThrow(() -> new BusinessException(IAMErrorCode.BRANCH_NOT_FOUND));
    }

    private BranchResponse save(BranchEntity branch) {
        branches.saveAndFlush(branch);
        em.refresh(branch);
        return response(branch);
    }

    private BranchResponse response(BranchEntity branch) {
        var result = mapper.toResponse(branch);
        result.setWorkspaceSlug(workspaces
                .findOne(WorkspaceSpecification.currentForBranch(branch.getId()))
                .map(WorkspaceEntity::getSlug)
                .orElse(null));
        return result;
    }

    private static void apply(BranchEntity b, BranchProfileRequest r) {
        try {
            ZoneId.of(r.getTimezone());
        } catch (DateTimeException ex) {
            throw new BusinessException(IAMErrorCode.BRANCH_NOT_READY, "Use a valid IANA timezone");
        }
        b.setName(SchoolAccess.clean(r.getName()));
        b.setAddressLine(SchoolAccess.clean(r.getAddressLine()));
        b.setDistrict(SchoolAccess.clean(r.getDistrict()));
        b.setProvince(SchoolAccess.clean(r.getProvince()));
        b.setCountryCode(r.getCountryCode());
        b.setLatitude(r.getLatitude());
        b.setLongitude(r.getLongitude());
        b.setTimezone(r.getTimezone());
    }
}
