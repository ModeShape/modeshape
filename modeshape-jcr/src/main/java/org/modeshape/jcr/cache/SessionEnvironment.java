/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.cache;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import javax.transaction.TransactionManager;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * 
 */
public interface SessionEnvironment {

    /**
     * Get the transaction manager for the session.
     * 
     * @return the transaction manager; never null
     */
    TransactionManager getTransactionManager();

    /**
     * Get an indexer that can be used to index the changes made within the supplied transaction context.
     * 
     * @return the indexer; may be null if no monitoring is to be performed
     */
    Monitor createMonitor();

    public static interface Monitor {
        /**
         * Add to the index the information about a node.
         * 
         * @param workspace the workspace in which the node information should be available; may not be null
         * @param key the unique key for the node; may not be null
         * @param path the path of the node; may not be null
         * @param primaryType the primary type of the node; may not be null
         * @param mixinTypes the mixin types for the node; may not be null but may be empty
         * @param properties the properties of the node; may not be null but may be empty
         */
        void recordAdd( String workspace,
                        NodeKey key,
                        Path path,
                        Name primaryType,
                        Set<Name> mixinTypes,
                        Collection<Property> properties );

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

}
