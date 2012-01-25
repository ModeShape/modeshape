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
package org.modeshape.jcr.query;

import java.util.Collection;
import java.util.Iterator;
import org.hibernate.search.backend.TransactionContext;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * Interface used to record in the indexes the changes to content.
 */
public interface QueryIndexing {

    /**
     * Add to the index the information about a node.
     * 
     * @param workspace the workspace in which the node information should be available; may not be null
     * @param key the unique key for the node; may not be null
     * @param path the path of the node; may not be null
     * @param properties the properties of the node; may not be null but may be empty
     * @param txnCtx the transaction context in which the index updates should be made; may not be null
     */
    void addToIndex( String workspace,
                     NodeKey key,
                     Path path,
                     Collection<Property> properties,
                     TransactionContext txnCtx );

    /**
     * Update the index to reflect the new state of the node.
     * 
     * @param workspace the workspace in which the node information should be available; may not be null
     * @param key the unique key for the node; may not be null
     * @param path the path of the node; may not be null
     * @param properties the properties of the node; may not be null but may be empty
     * @param txnCtx the transaction context in which the index updates should be made; may not be null
     */
    void updateIndex( String workspace,
                      NodeKey key,
                      Path path,
                      Iterator<Property> properties,
                      TransactionContext txnCtx );

    /**
     * Update the index to reflect the new state of the node.
     * 
     * @param workspace the workspace in which the node information should be available; may not be null
     * @param key the unique key for the node; may not be null
     * @param path the path of the node; may not be null
     * @param properties the properties of the node; may not be null but may be empty
     * @param txnCtx the transaction context in which the index updates should be made; may not be null
     */
    void updateIndex( String workspace,
                      NodeKey key,
                      Path path,
                      Collection<Property> properties,
                      TransactionContext txnCtx );

    void removeFromIndex( String workspace,
                          Iterable<NodeKey> keys,
                          TransactionContext txnCtx );

    void removeAllFromIndex( String workspace,
                             TransactionContext txnCtx );

    void addBinaryToIndex( Binary binary,
                           TransactionContext txnCtx );

    void removeBinariesFromIndex( Iterable<String> sha1s,
                                  TransactionContext txnCtx );

}
