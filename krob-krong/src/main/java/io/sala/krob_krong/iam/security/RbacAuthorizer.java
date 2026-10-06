package io.sala.krob_krong.iam.security;

import io.sala.krob_krong.audit.service.AccessLogService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("rbac")
@RequiredArgsConstructor
public class RbacAuthorizer {

    private final AccessPolicyService accessPolicy;
    private final AccessLogService accessLog;

    public boolean can(String permission, UUID schoolId, UUID branchId, UUID targetUserId) {
        UUID userId = Principals.currentUserId().orElse(null);
        boolean allowed = accessPolicy.canAccess(userId, permission, schoolId, branchId, targetUserId);
        if (!allowed || accessPolicy.isSensitive(permission)) {
            record(userId, permission, schoolId, branchId, targetUserId, allowed);
        }
        return allowed;
    }

    private void record(UUID userId, String permission, UUID schoolId, UUID branchId, UUID targetId, boolean allowed) {
        try {
            accessLog.record(userId, permission, schoolId, branchId, targetId, allowed);
        } catch (RuntimeException e) {
            log.error("Failed to write access log for permission {} (allowed={})", permission, allowed, e);
        }
    }
}
