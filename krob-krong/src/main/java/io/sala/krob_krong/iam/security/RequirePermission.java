package io.sala.krob_krong.iam.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@PreAuthorize("@rbac.can('{value}', {school}, {branch}, {target})")
public @interface RequirePermission {

    String value();

    String school() default "null";

    String branch() default "null";

    String target() default "null";
}
