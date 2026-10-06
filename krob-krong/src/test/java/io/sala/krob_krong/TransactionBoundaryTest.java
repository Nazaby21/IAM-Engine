package io.sala.krob_krong;

import static org.assertj.core.api.Assertions.assertThat;

import io.sala.krob_krong.account.repository.RefreshTokenRepository;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

class TransactionBoundaryTest {

    @Test
    void noBeanOpensATransactionForEveryMethod() {
        List<String> classLevel = beans().stream()
                .filter(type -> AnnotatedElementUtils.hasAnnotation(type, Transactional.class))
                .map(Class::getName)
                .toList();

        assertThat(classLevel).isEmpty();
    }

    @Test
    void controllersNeverOpenTransactions() {
        List<String> transactionalHandlers = beans().stream()
                .filter(type -> AnnotatedElementUtils.hasAnnotation(type, RestController.class))
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(method -> AnnotatedElementUtils.hasAnnotation(method, Transactional.class))
                .map(Method::toString)
                .toList();

        assertThat(transactionalHandlers).isEmpty();
    }

    @Test
    void refreshTokenRevocationCommitsIndependentlyOfItsCaller() throws NoSuchMethodException {
        Transactional revokeFamily = RefreshTokenRepository.class
                .getMethod("revokeFamily", UUID.class, Instant.class)
                .getAnnotation(Transactional.class);

        assertThat(revokeFamily.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    private static List<Class<?>> beans() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        return scanner.findCandidateComponents("io.sala.krob_krong").stream()
                .<Class<?>>map(definition -> load(definition.getBeanClassName()))
                .toList();
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
