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

import java.util.Iterator;
import java.util.Set;
import org.hibernate.search.backend.TransactionContext;
import org.modeshape.jcr.NodeTypeSchemata;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
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
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types for the node; may not be null but may be empty
     * @param propertiesIterator the iterator over the properties of a node; may not be null but may be empty
     * @param schemata the node type schemata that should be used to determine how the node is to be indexed; may not be null
     * @param txnCtx the transaction context in which the index updates should be made; may not be null
     */
    void addToIndex( String workspace,
                     NodeKey key,
                     Path path,
                     Name primaryType,
                     Set<Name> mixinTypes,
                     Iterator<Property> propertiesIterator,
                     NodeTypeSchemata schemata,
                     TransactionContext txnCtx );

    /**
     * Update the index to reflect the new state of the node.
     * 
     * @param workspace the workspace in which the node information should be available; may not be null
     * @param key the unique key for the node; may not be null
     * @param path the path of the node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types for the node; may not be null but may be empty
     * @param properties the properties of the node; may not be null but may be empty
     * @param schemata the node type schemata that should be used to determine how the node is to be indexed; may not be null
     * @param txnCtx the transaction context in which the index updates should be made; may not be null
     */
    void updateIndex( String workspace,
                      NodeKey key,
                      Path path,
                      Name primaryType,
                      Set<Name> mixinTypes,
                      Iterator<Property> properties,
                      NodeTypeSchemata schemata,
                      TransactionContext txnCtx );

    /**
     * Retrieve whether the indexes were initialized and empty upon startup.
     * 
     * @return true if the index(es) initially had no content, or false if there was already at least some content in the indexes
     *         upon startup
     */
    boolean initializedIndexes();

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
