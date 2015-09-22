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
package org.modeshape.jcr.spi.index;

import java.util.Set;
import org.modeshape.jcr.cache.CachedNode.Properties;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * Interface used to record in the indexes the changes to content.
 * 
 * @see IndexProvider#getIndexWriter()
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface IndexWriter {

    /**
     * Flag that defines whether this index may be skipped. This is usually the case when the writer has no indexes behind it.
     * 
     * @return true if this index writer does not need to be called, or false otherwise
     */
    boolean canBeSkipped();

    /**
     * Clear all indexes of content. This method is typically called prior to a re-indexing operation.
     */
    void clearAllIndexes();

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
    void add( String workspace,
              NodeKey key,
              Path path,
              Name primaryType,
              Set<Name> mixinTypes,
              Properties properties );

    /**
     * Removes information from the indexes about a node. 
     *
     * @param workspace the workspace to which the node belongs; may not be null
     * @param key a {@link NodeKey} instance, never {@code null}
     */
    void remove( String workspace, NodeKey key );

}
