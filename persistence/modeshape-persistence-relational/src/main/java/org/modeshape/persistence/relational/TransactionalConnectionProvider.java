/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.persistence.relational;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.modeshape.common.logging.Logger;

/**
 * Interface used by the {@link RelationalDb relational db} to handle database connections. Since the relational db can only
 * write as part of ongoing transactions (which are managed from the outside by ModeShape) it's very important that each transaction
 * uses a separate {@link Connection database connection} which only commits the changes once the transaction is committed.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public interface TransactionalConnectionProvider {

    Logger LOGGER = Logger.getLogger(TransactionalConnectionProvider.class);

    /**
     * Thread local holder for the current active transaction id.
     */
    ThreadLocal<String> ACTIVE_TX_ID = new InheritableThreadLocal<>();

    /**
     * Map of db connections per transaction id
     */
    Map<String, Connection> CONNECTIONS_BY_TX_ID = new HashMap<>();

    /**
     * Acquires a new database connection.
     *
     * @param autoCommit a {@code boolean} flag indicating if the connection should be in auto-commit mode or not.
     * @return a new {@link Connection}, never {@code null}
     */
    Connection newConnection(boolean autoCommit);

    /**
     * Returns the ID of the active thread-local transaction.
     *
     * @return a {@link Optional} transaction id.
     */
    default Optional<String> activeTransactionId() {
        return Optional.ofNullable(ACTIVE_TX_ID.get());
    }

    /**
     * Returns an existing {@link Connection} for the current transaction
     *
     * @return a {@link Connection} instance, never {@code null}
     * @throws RelationalProviderException if either there is no active transaction or if there is no connection for the active
     * transaction.
     */
    default Connection connectionForActiveTx() {
        String txId = requireActiveTransaction();
        Connection connection = CONNECTIONS_BY_TX_ID.get(txId);
        if (connection == null) {
            throw new RelationalProviderException(RelationalProviderI18n.threadNotAssociatedWithTransaction,
                                                  Thread.currentThread().getName());
        }
        return connection;
    }

    /**
     * Requires that an active transaction exists for the current calling thread.
     * 
     * @return the ID of the active transaction, never {@code null}
     * @throws RelationalProviderException if the current thread is not associated with a transaction.
     */
    default String requireActiveTransaction() {
        return activeTransactionId().orElseThrow(() -> new RelationalProviderException(
                RelationalProviderI18n.threadNotAssociatedWithTransaction,
                Thread.currentThread().getName()));
    }

    /**
     * Creates and stores a new {@link Connection database connection} for the active transaction. This method effectively
     * binds to the current thread a particular transaction.
     *
     * @param txId a the id of a ModeShape transaction
     * @see #releaseConnection(String)
     */
    default void allocateConnection(String txId) {
        String currentTx = ACTIVE_TX_ID.get();
        if (currentTx != null && !currentTx.equals(txId)) {
            throw new RelationalProviderException(RelationalProviderI18n.threadAssociatedWithAnotherTransaction, 
                                                  Thread.currentThread().getName(), currentTx);
        }
        ACTIVE_TX_ID.set(txId);
        CONNECTIONS_BY_TX_ID.putIfAbsent(txId, newConnection(false));
    }

    /**
     * Closes and removes an active connection for a given transaction. The connection should've been created previously
     * off the same thread by calling {@link #allocateConnection(String)}
     *
     * @param txId a the id of a ModeShape transaction
     * @throws RelationalProviderException if this method is called from a thread for which either there is no transaction
     * or there is a different transaction.
     * @see #allocateConnection(String)
     */
    default void releaseConnection(String txId) {
        String currentTx = ACTIVE_TX_ID.get();
        if (!currentTx.equals(txId)) {
            throw new RelationalProviderException(RelationalProviderI18n.threadAssociatedWithAnotherTransaction,
                                                  Thread.currentThread().getName(), currentTx);
        }
        ACTIVE_TX_ID.remove();
        try (Connection connection = CONNECTIONS_BY_TX_ID.remove(txId)) {
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        }
    }

    /**
     * Closes and removes all connections held by this provider.
     */
    default void clearAllConnections() {
        ACTIVE_TX_ID.remove();
        CONNECTIONS_BY_TX_ID.values().stream().forEach(connection -> {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.debug(e, "Cannot close connection..");
            }
        });
        CONNECTIONS_BY_TX_ID.clear();
    }
}
