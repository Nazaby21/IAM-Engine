package io.sala.krob_krong.iam.config.security;

import io.sala.krob_krong.common.errors.CommonErrorCode;
import io.sala.krob_krong.common.exceptions.BusinessException;
import java.util.Optional;
import org.springframework.util.StringUtils;

public final class Principals {

    public static final String CLAIM_TENANT = "tenant";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_PLATFORM_ADMIN = "platform_admin";

    private Principals() {}

    public static Optional<SecurityContext> current() {
        return SecurityContextHolder.optional();
    }

    public static String requireUserId() {
        return current()
                .map(SecurityContext::principalId)
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED, "Not authenticated"));
    }

    public static Optional<String> userId() {
        return current().map(SecurityContext::principalId).filter(StringUtils::hasText);
    }

    public static String requireTenantId() {
        return tenantId()
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.FORBIDDEN, "No active workspace selected for this session"));
    }

    public static Optional<String> tenantId() {
        return claim(CLAIM_TENANT);
    }

    public static boolean isPlatformAdmin() {
        return claim(CLAIM_PLATFORM_ADMIN).map("true"::equalsIgnoreCase).orElse(false);
    }

    public static Optional<String> claim(String name) {
        return current().map(ctx -> ctx.claims().get(name)).filter(StringUtils::hasText);
    }

    public static boolean hasPermission(String permission) {
        return current().map(ctx -> ctx.hasPermission(permission)).orElse(false);
    }
}
