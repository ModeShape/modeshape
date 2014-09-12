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
import java.util.NoSuchElementException;
import org.modeshape.common.collection.ReadOnlyIterator;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinitionTemplate;

class RepositoryIndexDefinitionTemplate implements IndexDefinitionTemplate {

    private final String DEFAULT_NODE_TYPE_NAME = JcrNtLexicon.BASE.getString();
    private final boolean DEFAULT_SYNCHRONOUS = true;
    private final boolean DEFAULT_ENABLED = true;

    private String name;
    private String providerName;
    private IndexKind kind = IndexKind.VALUE;
    private String nodeTypeName = DEFAULT_NODE_TYPE_NAME;
    private String description = "";
    private boolean enabled = DEFAULT_ENABLED;
    private boolean synchronous = DEFAULT_SYNCHRONOUS;
    private List<IndexColumnDefinition> columnDefns = new ArrayList<>();
    private Map<String, Object> extendedProperties = new HashMap<>();
    private WorkspaceMatchRule workspaceRule = RepositoryIndexDefinition.MATCH_ALL_WORKSPACES_RULE;

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
    public String getNodeTypeName() {
        return nodeTypeName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isSynchronous() {
        return synchronous;
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
    public int size() {
        return columnDefns.size();
    }

    @Override
    public IndexColumnDefinition getColumnDefinition( int position ) throws NoSuchElementException {
        return columnDefns.get(position);
    }

    @Override
    public Object getIndexProperty( String propertyName ) {
        return extendedProperties.get(propertyName);
    }

    @Override
    public Map<String, Object> getIndexProperties() {
        return extendedProperties;
    }

    @Override
    public WorkspaceMatchRule getWorkspaceMatchRule() {
        return workspaceRule;
    }

    @Override
    public Iterator<IndexColumnDefinition> iterator() {
        return ReadOnlyIterator.around(columnDefns.iterator());
    }

    @Override
    public boolean appliesToProperty( String propertyName ) {
        for (IndexColumnDefinition columnDefn : this) {
            if (columnDefn.getPropertyName().equals(propertyName)) return true;
        }
        return false;
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
    public IndexDefinitionTemplate setNodeTypeName( String name ) {
        this.nodeTypeName = name != null ? name : DEFAULT_NODE_TYPE_NAME;
        return this;
    }

    @Override
    public IndexDefinitionTemplate setDescription( String description ) {
        this.description = description != null ? description : "";
        return this;
    }

    @Override
    public IndexDefinitionTemplate setSynchronous( boolean synchronous ) {
        this.synchronous = synchronous;
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

    @Override
    public IndexDefinitionTemplate setColumnDefinitions( IndexColumnDefinition columnDefinition ) {
        this.columnDefns.clear();
        this.columnDefns.add(RepositoryIndexColumnDefinition.createFrom(columnDefinition));
        return this;
    }

    @Override
    public IndexDefinitionTemplate setColumnDefinitions( IndexColumnDefinition firstColumnDefinition,
                                                         IndexColumnDefinition... additionalColumnDefinitions ) {
        this.columnDefns.clear();
        this.columnDefns.add(RepositoryIndexColumnDefinition.createFrom(firstColumnDefinition));
        for (IndexColumnDefinition defn : additionalColumnDefinitions) {
            this.columnDefns.add(RepositoryIndexColumnDefinition.createFrom(defn));
        }
        return this;
    }

    @Override
    public IndexDefinitionTemplate setAllWorkspaces() {
        this.workspaceRule = RepositoryIndexDefinition.MATCH_ALL_WORKSPACES_RULE;
        return this;
    }

    @Override
    public IndexDefinitionTemplate setWorkspace( String workspaceName ) {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceRule = new RepositoryIndexDefinition.ExactWorkspaceMatchRule(workspaceName);
        return this;
    }

    @Override
    public IndexDefinitionTemplate setWorkspaces( String... workspaceNames ) {
        CheckArg.isNotEmpty(workspaceNames, "workspaceNames");
        this.workspaceRule = RepositoryIndexDefinition.workspaceMatchRule(workspaceNames);
        return this;
    }

    @Override
    public IndexDefinitionTemplate setWorkspaceNamePattern( String regex ) {
        this.workspaceRule = new RepositoryIndexDefinition.ExactWorkspaceMatchRule(regex);
        return this;
    }

}
