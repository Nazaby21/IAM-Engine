package io.sala.krob_krong.iam.security;

import java.util.Optional;

public final class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> CONTEXT = new ThreadLocal<>();

    private SecurityContextHolder() {}

    public static SecurityContext get() {
        SecurityContext ctx = CONTEXT.get();
        if (ctx == null) {
            throw new IllegalStateException("No SecurityContext available in current thread");
        }
        return ctx;
    }

    public static void set(SecurityContext context) {
        CONTEXT.set(context);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static Optional<SecurityContext> optional() {
        return Optional.ofNullable(CONTEXT.get());
    }
}
