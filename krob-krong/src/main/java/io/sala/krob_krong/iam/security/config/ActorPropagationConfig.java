package io.sala.krob_krong.iam.security.config;

import io.sala.krob_krong.iam.security.persistence.ActorAwareJpaTransactionManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;

@Configuration
public class ActorPropagationConfig {

    // Replaces Boot's default transaction manager, which backs off when one is defined.
    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new ActorAwareJpaTransactionManager(entityManagerFactory);
    }
}
