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
package org.modeshape.jcr.cache;

import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.cache.document.TransactionalWorkspaceCaches;
import org.modeshape.jcr.txn.Transactions;

/**
 * Interface which exposes global repository subsystems/configuration to running sessions.
 */
public interface RepositoryEnvironment {

    /**
     * Get the interface for working with transactions.
     * 
     * @return the transactions object
     */
    Transactions getTransactions();

    /**
     * Get the factory used to obtain the transactional workspace caches.
     * 
     * @return the factory; never null
     */
    TransactionalWorkspaceCaches getTransactionalWorkspaceCacheFactory();

    /**
     * Returns the id of the repository's {@link org.modeshape.jcr.journal.ChangeJournal}
     * 
     * @return either a {@link String} or {@code null} if no journal is configured.
     */
    String journalId();

    /**
     * Returns the {@link NodeTypes} instance for this repository.
     *
     * @return a {@link NodeTypes} instance or {@code null} if no such information is available yet (e.g. the node types have not
     * been initialized yet).
     */
    NodeTypes nodeTypes();
}
