package io.sala.krob_krong.audit.service.impl;

import io.sala.krob_krong.audit.entity.AccessLogEntity;
import io.sala.krob_krong.audit.repository.AccessLogRepository;
import io.sala.krob_krong.audit.service.AccessLogService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessLogServiceImpl implements AccessLogService {

    private final AccessLogRepository accessLogRepository;

    // REQUIRES_NEW: a denial usually aborts the caller's work, and the log entry must survive that rollback.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID userId, String permissionCode, UUID schoolId, UUID branchId, UUID targetId, boolean allowed) {
        AccessLogEntity entry = new AccessLogEntity();
        entry.setAt(Instant.now());
        entry.setUserId(userId);
        entry.setPermissionCode(permissionCode);
        entry.setSchoolId(schoolId);
        entry.setBranchId(branchId);
        entry.setTargetId(targetId);
        entry.setAllowed(allowed);
        accessLogRepository.save(entry);
    }
}
