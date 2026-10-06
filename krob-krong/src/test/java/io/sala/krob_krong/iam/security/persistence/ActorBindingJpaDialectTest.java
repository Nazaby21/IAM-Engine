package io.sala.krob_krong.iam.security.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.sala.krob_krong.iam.security.AuthenticatedUser;
import io.sala.krob_krong.iam.security.jwt.UserAuthenticationToken;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

class ActorBindingJpaDialectTest {

    private final JpaDialect delegate = mock(JpaDialect.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final Session session = mock(Session.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement statement = mock(PreparedStatement.class);
    private final TransactionDefinition definition = new DefaultTransactionDefinition();
    private final ActorBindingJpaDialect dialect = new ActorBindingJpaDialect(delegate);

    @BeforeEach
    void setUp() throws Exception {
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        doAnswer(invocation -> {
                    invocation.<Work>getArgument(0).execute(connection);
                    return null;
                })
                .when(session)
                .doWork(any());
        when(connection.prepareStatement(ActorBindingJpaDialect.SET_ACTOR_SQL)).thenReturn(statement);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bindsAuthenticatedUserAsTransactionLocalActorAfterBegin() throws Exception {
        UUID actorId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(authenticated(actorId));
        Object transactionData = new Object();
        when(delegate.beginTransaction(entityManager, definition)).thenReturn(transactionData);

        assertThat(dialect.beginTransaction(entityManager, definition)).isSameAs(transactionData);

        InOrder order = inOrder(delegate, statement);
        order.verify(delegate).beginTransaction(entityManager, definition);
        order.verify(statement).setString(1, actorId.toString());
        order.verify(statement).execute();
    }

    @Test
    void anonymousTransactionsLeaveActorUnset() throws Exception {
        dialect.beginTransaction(entityManager, definition);

        verifyNoInteractions(session);
    }

    @Test
    void readOnlyTransactionsSkipTheActorStatement() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authenticated(UUID.randomUUID()));
        DefaultTransactionDefinition readOnly = new DefaultTransactionDefinition();
        readOnly.setReadOnly(true);

        dialect.beginTransaction(entityManager, readOnly);

        verifyNoInteractions(session);
    }

    @Test
    void transactionManagerWrapsBootsDialectExactlyOnce() {
        EntityManagerFactory emf =
                mock(EntityManagerFactory.class, withSettings().extraInterfaces(EntityManagerFactoryInfo.class));
        when(((EntityManagerFactoryInfo) emf).getJpaDialect()).thenReturn(new HibernateJpaDialect());

        ActorAwareJpaTransactionManager transactionManager = new ActorAwareJpaTransactionManager(emf);
        transactionManager.afterPropertiesSet();

        assertThat(transactionManager.getJpaDialect()).isInstanceOf(ActorBindingJpaDialect.class);
    }

    private static UserAuthenticationToken authenticated(UUID id) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(id.toString())
                .build();
        AuthenticatedUser user =
                new AuthenticatedUser(id, UUID.randomUUID(), AuthenticatedUser.KIND_PERSON, null, null, false);
        return new UserAuthenticationToken(jwt, user, List.of());
    }
}
