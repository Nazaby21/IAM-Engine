package io.sala.krob_krong.audit.service;

import java.util.UUID;

public interface AccessLogService {

    void record(UUID userId, String permissionCode, UUID schoolId, UUID branchId, UUID targetId, boolean allowed);
}
