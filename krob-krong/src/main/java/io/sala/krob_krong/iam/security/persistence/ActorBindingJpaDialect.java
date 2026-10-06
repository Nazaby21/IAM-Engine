package io.sala.krob_krong.iam.security.persistence;

import io.sala.krob_krong.iam.security.Principals;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.hibernate.Session;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

// Sets transaction-local app.actor_id for the write guards; done in the dialect so a failed bind gets begin cleanup.
public final class ActorBindingJpaDialect implements JpaDialect {

    static final String SET_ACTOR_SQL = "SELECT set_config('app.actor_id', ?, true)";

    private final JpaDialect delegate;

    public ActorBindingJpaDialect(JpaDialect delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
            throws PersistenceException, SQLException, TransactionException {
        Object transactionData = delegate.beginTransaction(entityManager, definition);
        if (!definition.isReadOnly()) {
            Principals.currentUserId().ifPresent(actorId -> bindActor(entityManager, actorId));
        }
        return transactionData;
    }

    private static void bindActor(EntityManager entityManager, UUID actorId) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SET_ACTOR_SQL)) {
                statement.setString(1, actorId.toString());
                statement.execute();
            }
        });
    }

    @Override
    public Object prepareTransaction(EntityManager entityManager, boolean readOnly, String name)
            throws PersistenceException {
        return delegate.prepareTransaction(entityManager, readOnly, name);
    }

    @Override
    public void cleanupTransaction(Object transactionData) {
        delegate.cleanupTransaction(transactionData);
    }

    @Override
    public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
            throws PersistenceException, SQLException {
        return delegate.getJdbcConnection(entityManager, readOnly);
    }

    @Override
    public void releaseJdbcConnection(ConnectionHandle conHandle, EntityManager entityManager)
            throws PersistenceException, SQLException {
        delegate.releaseJdbcConnection(conHandle, entityManager);
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return delegate.translateExceptionIfPossible(ex);
    }
}
