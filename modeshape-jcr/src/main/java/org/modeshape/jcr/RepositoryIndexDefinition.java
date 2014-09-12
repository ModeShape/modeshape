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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.ReadOnlyIterator;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinition;

@Immutable
final class RepositoryIndexDefinition implements IndexDefinition {

    private static final Logger LOGGER = Logger.getLogger(RepositoryIndexDefinition.class);

    public static IndexDefinition createFrom( IndexDefinition other ) {
        return new RepositoryIndexDefinition(other.getName(), other.getProviderName(), other.getKind(), other.getNodeTypeName(),
                                             other, other.getIndexProperties(), other.getDescription(), other.isSynchronous(),
                                             other.isEnabled(), other.getWorkspaceMatchRule());
    }

    public static IndexDefinition createFrom( IndexDefinition other,
                                              boolean isEnabled ) {
        return new RepositoryIndexDefinition(other.getName(), other.getProviderName(), other.getKind(), other.getNodeTypeName(),
                                             other, other.getIndexProperties(), other.getDescription(), other.isSynchronous(),
                                             isEnabled, other.getWorkspaceMatchRule());
    }

    private final String name;
    private final String providerName;
    private final IndexKind kind;
    private final String nodeTypeName;
    private final String description;
    private final boolean synchronous;
    private final boolean enabled;
    private final List<IndexColumnDefinition> columnDefns;
    private final Map<String, IndexColumnDefinition> columnDefnsByName;
    private final Map<String, Object> extendedProperties;
    private final WorkspaceMatchRule workspaceRule;

    RepositoryIndexDefinition( String name,
                               String providerName,
                               IndexKind kind,
                               String nodeTypeName,
                               Iterable<IndexColumnDefinition> columnDefns,
                               Map<String, Object> extendedProperties,
                               String description,
                               boolean synchronous,
                               boolean enabled,
                               WorkspaceMatchRule workspaceRule ) {
        assert name != null;
        assert providerName != null;
        assert columnDefns != null;
        assert extendedProperties != null;
        assert workspaceRule != null;
        this.name = name;
        this.providerName = providerName;
        this.kind = kind;
        this.nodeTypeName = nodeTypeName != null ? nodeTypeName : JcrNtLexicon.BASE.getString();
        this.columnDefns = new ArrayList<>();
        this.extendedProperties = extendedProperties;
        this.description = description != null ? description : "";
        this.enabled = enabled;
        this.synchronous = synchronous;
        this.workspaceRule = workspaceRule;
        this.columnDefnsByName = new HashMap<>();
        for (IndexColumnDefinition columnDefn : columnDefns) {
            assert columnDefn != null;
            this.columnDefns.add(columnDefn);
            this.columnDefnsByName.put(columnDefn.getPropertyName(), columnDefn);
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
        return Collections.unmodifiableMap(extendedProperties);
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
        return columnDefnsByName.containsKey(propertyName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append('@').append(getProviderName());
        sb.append(" nodeType=").append(nodeTypeName);
        if (columnDefns.size() == 1) {
            sb.append(" column=");
        } else {
            sb.append(" columns=");
        }
        boolean first = true;
        for (IndexColumnDefinition col : columnDefns) {
            if (first) first = false;
            else sb.append(",");
            sb.append(col);
        }
        sb.append(" kind=").append(getKind());
        sb.append(" sync=").append(isSynchronous());
        sb.append(" workspaces=").append(workspaceRule);
        return sb.toString();
    }

    protected static WorkspaceMatchRule workspaceMatchRule( String... workspaceNames ) {
        if (workspaceNames == null || workspaceNames.length == 0) return MATCH_ALL_WORKSPACES_RULE;
        Set<String> names = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        for (String name : workspaceNames) {
            name = name.trim();
            if (name.length() != 0) {
                if (names.size() != 0) sb.append(",");
                names.add(name);
                sb.append(name);
            }
        }
        if (!names.isEmpty()) {
            return new MultipleWorkspaceMatchRule(sb.toString(), names);
        }
        return MATCH_ALL_WORKSPACES_RULE;
    }

    public static WorkspaceMatchRule workspaceMatchRule( String rule ) {
        if (rule == null) return MATCH_ALL_WORKSPACES_RULE;
        rule = rule.trim();
        if (rule.length() == 0 || MATCH_ALL_WORKSPACES.equals(rule)) return MATCH_ALL_WORKSPACES_RULE;
        try {
            return new RegexWorkspaceMatchRule(rule, Pattern.compile(rule));
        } catch (PatternSyntaxException e) {
            LOGGER.debug("Unable to parse workspace rule '{0}' into regular expression", rule);
        }
        try {
            String[] names = rule.split(",");
            Set<String> workspaceNames = new HashSet<>();
            for (String name : names) {
                if (name.trim().length() != 0) workspaceNames.add(name.trim());
            }
            if (!workspaceNames.isEmpty()) return new MultipleWorkspaceMatchRule(rule, workspaceNames);
        } catch (PatternSyntaxException e) {
            LOGGER.debug("Unable to parse workspace rule '{0}' into comma-separate list of workspace names", rule);
        }
        return new ExactWorkspaceMatchRule(rule);
    }

    public static final String MATCH_ALL_WORKSPACES = "*";
    protected static final WorkspaceMatchRule MATCH_ALL_WORKSPACES_RULE = new MatchAllWorkspaces();

    protected static class MatchAllWorkspaces implements WorkspaceMatchRule {
        @Override
        public boolean usedInWorkspace( String workspaceName ) {
            return true;
        }

        @Override
        public String getDefinition() {
            return MATCH_ALL_WORKSPACES;
        }

        @Override
        public String toString() {
            return getDefinition();
        }
    }

    protected static class RegexWorkspaceMatchRule implements WorkspaceMatchRule {
        private final String rule;
        private final Pattern pattern;

        protected RegexWorkspaceMatchRule( String rule,
                                           Pattern pattern ) {
            this.rule = rule;
            this.pattern = pattern;
        }

        @Override
        public boolean usedInWorkspace( String workspaceName ) {
            return pattern.matcher(workspaceName).matches();
        }

        @Override
        public String getDefinition() {
            return rule;
        }

        @Override
        public String toString() {
            return getDefinition();
        }
    }

    protected static class ExactWorkspaceMatchRule implements WorkspaceMatchRule {
        private final String workspaceName;

        protected ExactWorkspaceMatchRule( String workspaceName ) {
            this.workspaceName = workspaceName;
        }

        @Override
        public boolean usedInWorkspace( String workspaceName ) {
            return this.workspaceName.equals(workspaceName);
        }

        @Override
        public String getDefinition() {
            return workspaceName;
        }

        @Override
        public String toString() {
            return getDefinition();
        }
    }

    protected static class MultipleWorkspaceMatchRule implements WorkspaceMatchRule {
        private final String rule;
        private final Set<String> workspaceNames;

        protected MultipleWorkspaceMatchRule( String rule,
                                              Set<String> workspaceNames ) {
            this.rule = rule;
            this.workspaceNames = workspaceNames;
        }

        @Override
        public boolean usedInWorkspace( String workspaceName ) {
            return this.workspaceNames.contains(workspaceName);
        }

        @Override
        public String getDefinition() {
            return rule;
        }

        @Override
        public String toString() {
            return getDefinition();
        }
    }

}
