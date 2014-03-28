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

import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.value.Name;

/**
 * A mutable form of the {@link IndexDefinition}.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface IndexDefinitionTemplate extends IndexDefinition {

    /**
     * Get the name of this index.
     * 
     * @return the name of this index; null if the value has not yet been set on this template
     */
    @Override
    String getName();

    /**
     * Get the name of the provider that owns this index.
     * 
     * @return the provider name; null if the value has not yet been set on this template
     */
    @Override
    String getProviderName();

    /**
     * Set the name of the index.
     * 
     * @param name the index name; may not be null
     * @return this instance for method chaining; never null
     */
    IndexDefinitionTemplate setName( String name );

    /**
     * Set the name of the {@link IndexProvider provider} that owns this index.
     * 
     * @param providerName the provider's name; may not be null
     * @return this instance for method chaining; never null
     */
    IndexDefinitionTemplate setProviderName( String providerName );

    /**
     * Set the kind of this index.
     * 
     * @param kind the kind; may not be null
     * @return this instance for method chaining; never null
     */
    IndexDefinitionTemplate setKind( IndexKind kind );

    /**
     * Set the name of the node type for which this index applies.
     * 
     * @param name the name of the node type for which this index applies; may be null if the property applies to all node types
     *        (e.g., {@code nt:base}).
     * @return this instance for method chaining; never null
     */
    IndexDefinitionTemplate setNodeTypeName( Name name );

    /**
     * Set the description for this index.
     * 
     * @param description the description; may be null or empty
     * @return this instance for method chaining; never null
     */
    IndexDefinitionTemplate setDescription( String description );

    /**
     * Set the definitions for this index's columns
     * 
     * @param columnDefinitions the definitions for the columns; may not be null or empty
     * @return this instance for method chaining; never null
     */
    IndexDefinitionTemplate setColumnDefinitions( Iterable<? extends IndexColumnDefinition> columnDefinitions );
}
