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
