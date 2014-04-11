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

import java.util.Map;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * An immutable description of an index definition.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
@Immutable
public interface IndexDefinition extends Iterable<IndexColumnDefinition> {

    /**
     * The kind of indexes.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static enum IndexKind {
        DUPLICATES,
        UNIQUE,
        ENUMERATED,
        FULLTEXTSEARCH,
        NODETYPE,
    }

    /**
     * Get the name of this index.
     * 
     * @return the name of this index; never null
     */
    String getName();

    /**
     * Get the name of the provider that owns this index.
     * 
     * @return the provider name; never null
     */
    String getProviderName();

    /**
     * Get the kind of index.
     * 
     * @return the index kind; never null
     */
    IndexKind getKind();

    /**
     * Get the name of the node type for which this index applies.
     * 
     * @return the node type name; never null
     */
    Name getNodeTypeName();

    /**
     * Get the description of this index.
     * 
     * @return the description; never null
     */
    String getDescription();

    /**
     * Determine whether this index is currently enabled.
     * 
     * @return true if this index is enabled and active, or false if it is not currently being used
     */
    boolean isEnabled();

    /**
     * Determine whether this index has a single column.
     * 
     * @return true if this index has a single column, or false if it has more
     */
    boolean hasSingleColumn();

    /**
     * Get the index property with the given name. These properties are usually non-standard.
     * 
     * @param propertyName the property name; may not be null
     * @return the property, or null if there is no property with that name
     */
    Property getProperty( Name propertyName );

    /**
     * Get the index properties keyed by their name. These properties are usually non-standard.
     * 
     * @return the properties; never null but possibly empty
     */
    Map<Name, Property> getProperties();
}
