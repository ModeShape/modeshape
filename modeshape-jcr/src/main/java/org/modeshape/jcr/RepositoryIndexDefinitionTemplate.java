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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.modeshape.common.collection.ReadOnlyIterator;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.spi.index.IndexColumnDefinition;
import org.modeshape.jcr.spi.index.IndexDefinitionTemplate;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

class RepositoryIndexDefinitionTemplate implements IndexDefinitionTemplate {

    private String name;
    private String providerName;
    private IndexKind kind = IndexKind.DUPLICATES;
    private Name nodeTypeName = JcrNtLexicon.BASE;
    private String description = "";
    private boolean enabled = true;
    private List<IndexColumnDefinition> columnDefns = new ArrayList<>();
    private Map<Name, Property> extendedProperties = new HashMap<>();

    RepositoryIndexDefinitionTemplate() {
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
        return extendedProperties;
    }

    @Override
    public Iterator<IndexColumnDefinition> iterator() {
        return new ReadOnlyIterator<>(columnDefns.iterator());
    }

    @Override
    public IndexDefinitionTemplate setName( String name ) {
        CheckArg.isNotNull(name, "name");
        this.name = name;
        return this;
    }

    @Override
    public IndexDefinitionTemplate setProviderName( String providerName ) {
        CheckArg.isNotNull(providerName, "providerName");
        this.providerName = providerName;
        return this;
    }

    @Override
    public IndexDefinitionTemplate setKind( IndexKind kind ) {
        CheckArg.isNotNull(kind, "kind");
        this.kind = kind;
        return this;
    }

    @Override
    public IndexDefinitionTemplate setNodeTypeName( Name name ) {
        this.nodeTypeName = name != null ? name : JcrNtLexicon.BASE;
        return this;
    }

    @Override
    public IndexDefinitionTemplate setDescription( String description ) {
        this.description = description != null ? description : "";
        return this;
    }

    @Override
    public IndexDefinitionTemplate setColumnDefinitions( Iterable<? extends IndexColumnDefinition> columnDefinitions ) {
        this.columnDefns.clear();
        for (IndexColumnDefinition defn : columnDefinitions) {
            this.columnDefns.add(RepositoryIndexColumnDefinition.createFrom(defn));
        }
        return this;
    }
}
