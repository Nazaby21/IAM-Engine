package io.sala.krob_krong.school.service;

import io.sala.krob_krong.common.errors.CommonErrorCode;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.iam.security.*;
import io.sala.krob_krong.school.entity.SchoolEntity;
import io.sala.krob_krong.school.repository.SchoolRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchoolAccess {
    private final SchoolRepository schools;
    private final AccessPolicyService policy;

    public SchoolEntity read(UUID id, String permission) {
        SchoolEntity school =
                schools.findById(id).orElseThrow(() -> new BusinessException(IAMErrorCode.SCHOOL_NOT_FOUND));
        check(id, permission);
        return school;
    }

    /** Caller owns a write transaction. All onboarding writers lock the school first. */
    public SchoolEntity lock(UUID id, String permission) {
        SchoolEntity school =
                schools.findByIdForUpdate(id).orElseThrow(() -> new BusinessException(IAMErrorCode.SCHOOL_NOT_FOUND));
        check(id, permission);
        return school;
    }

    public void check(UUID id, String permission) {
        if (!policy.canAccess(Principals.currentUserId().orElse(null), permission, id, null, null)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    public static UUID actor() {
        return Principals.currentUserId().orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));
    }

    public static void editable(SchoolEntity school) {
        if ("pending_review".equals(school.getStatus()))
            throw new BusinessException(IAMErrorCode.SCHOOL_PROFILE_LOCKED);
        if (!Set.of("draft", "changes_requested", "approved").contains(school.getStatus()))
            throw new BusinessException(IAMErrorCode.SCHOOL_STATUS_INVALID);
    }

    public static String clean(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
