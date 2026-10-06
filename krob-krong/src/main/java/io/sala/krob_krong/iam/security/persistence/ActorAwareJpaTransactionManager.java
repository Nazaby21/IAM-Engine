package io.sala.krob_krong.iam.security.persistence;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;

public class ActorAwareJpaTransactionManager extends JpaTransactionManager {

    public ActorAwareJpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    // super.afterPropertiesSet() resets the dialect from the EntityManagerFactory, and the constructor also calls it.
    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        if (!(getJpaDialect() instanceof ActorBindingJpaDialect)) {
            setJpaDialect(new ActorBindingJpaDialect(getJpaDialect()));
        }
    }
}
