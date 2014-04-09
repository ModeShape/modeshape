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

import java.util.Iterator;
import java.util.Set;
import org.modeshape.jcr.cache.document.TransactionalWorkspaceCaches;
import org.modeshape.jcr.txn.Transactions;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * Interface which exposes global repository subsystems/configuration to running sessions.
 */
public interface SessionEnvironment {

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

    public static interface Monitor {
        /**
         * Add to the index the information about a node.
         * 
         * @param workspace the workspace in which the node information should be available; may not be null
         * @param key the unique key for the node; may not be null
         * @param path the path of the node; may not be null
         * @param primaryType the primary type of the node; may not be null
         * @param mixinTypes the mixin types for the node; may not be null but may be empty
         * @param propertiesIterator an iterator over a collection of properties
         */
        void recordAdd( String workspace,
                        NodeKey key,
                        Path path,
                        Name primaryType,
                        Set<Name> mixinTypes,
                        Iterator<Property> propertiesIterator );

        /**
         * Update the index to reflect the new state of the node.
         * 
         * @param workspace the workspace in which the node information should be available; may not be null
         * @param key the unique key for the node; may not be null
         * @param path the path of the node; may not be null
         * @param primaryType the primary type of the node; may not be null
         * @param mixinTypes the mixin types for the node; may not be null but may be empty
         * @param properties the properties of the node; may not be null but may be empty
         */
        void recordUpdate( String workspace,
                           NodeKey key,
                           Path path,
                           Name primaryType,
                           Set<Name> mixinTypes,
                           Iterator<Property> properties );

        /**
         * Remove from the index for the given workspace all of the nodes with the supplied keys.
         * 
         * @param workspace the workspace in which the nodes were removed; may not be null
         * @param keys the keys for the nodes that are to be removed; may not be null
         */
        void recordRemove( String workspace,
                           Iterable<NodeKey> keys );

        /**
         * Record total number of nodes that were affected
         * 
         * @param changedNodesCount
         */
        void recordChanged( long changedNodesCount );
    }

    /**
     * A simple interface used to construct {@link Monitor} instances.
     */
    public static interface MonitorFactory {
        /**
         * Get an indexer that can be used to index the changes made within the supplied transaction context.
         * 
         * @return the indexer; may be null if no monitoring is to be performed
         */
        Monitor createMonitor();
    }

}
