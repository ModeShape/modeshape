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

package org.modeshape.jcr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.ReadOnlyIterator;
import org.modeshape.jcr.spi.index.IndexColumnDefinition;
import org.modeshape.jcr.spi.index.IndexDefinition;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

@Immutable
class RepositoryIndexDefinition implements IndexDefinition {

    public static IndexDefinition createFrom( IndexDefinition other ) {
        return new RepositoryIndexDefinition(other.getName(), other.getProviderName(), other.getKind(), other.getNodeTypeName(),
                                             other, other.getProperties(), other.getDescription(), other.isEnabled());
    }

    public static IndexDefinition createFrom( IndexDefinition other,
                                              boolean isEnabled ) {
        return new RepositoryIndexDefinition(other.getName(), other.getProviderName(), other.getKind(), other.getNodeTypeName(),
                                             other, other.getProperties(), other.getDescription(), isEnabled);
    }

    private final String name;
    private final String providerName;
    private final IndexKind kind;
    private final Name nodeTypeName;
    private final String description;
    private final boolean enabled;
    private final List<IndexColumnDefinition> columnDefns;
    private final Map<Name, Property> extendedProperties;

    RepositoryIndexDefinition( String name,
                               String providerName,
                               IndexKind kind,
                               Name nodeTypeName,
                               Iterable<IndexColumnDefinition> columnDefns,
                               Map<Name, Property> extendedProperties,
                               String description,
                               boolean enabled ) {
        assert name != null;
        assert providerName != null;
        assert columnDefns != null;
        assert extendedProperties != null;
        this.name = name;
        this.providerName = providerName;
        this.kind = kind;
        this.nodeTypeName = nodeTypeName != null ? nodeTypeName : JcrNtLexicon.BASE;
        this.columnDefns = new ArrayList<>();
        this.extendedProperties = extendedProperties;
        this.description = description != null ? description : "";
        this.enabled = enabled;
        for (IndexColumnDefinition columnDefn : columnDefns) {
            assert columnDefn != null;
            this.columnDefns.add(columnDefn);
        }
        assert !this.columnDefns.isEmpty();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public IndexKind getKind() {
        return kind;
    }

    @Override
    public Name getNodeTypeName() {
        return nodeTypeName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean hasSingleColumn() {
        return columnDefns.size() == 1;
    }

    @Override
    public Property getProperty( Name propertyName ) {
        return extendedProperties.get(propertyName);
    }

    @Override
    public Map<Name, Property> getProperties() {
        return Collections.unmodifiableMap(extendedProperties);
    }

    @Override
    public Iterator<IndexColumnDefinition> iterator() {
        return new ReadOnlyIterator<>(columnDefns.iterator());
    }
}
