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
package org.modeshape.jdbc.rest;

import java.util.HashSet;
import java.util.Set;
import javax.jcr.version.OnParentVersionAction;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.HashCode;

/**
 * A {@link javax.jcr.nodetype.NodeDefinition} implementation for the Modeshape client.
 */
@Immutable
public class ChildNodeDefinition extends ItemDefinition implements javax.jcr.nodetype.NodeDefinition {

    private final Id id;
    private final String defaultPrimaryTypeName;

    protected ChildNodeDefinition( String declaringNodeTypeName,
                                   JSONObject json,
                                   NodeTypes nodeTypes ) {
        super(declaringNodeTypeName, json, nodeTypes);

        String name = JSONHelper.valueFrom(json, "jcr:name", "*");
        boolean allowsSns = JSONHelper.valueFrom(json, "jcr:sameNameSiblings", false);
        Set<String> requiredTypes = new HashSet<>(JSONHelper.valuesFrom(json, "jcr:requiredPrimaryTypes"));
        this.id = new Id(name, allowsSns, requiredTypes);
        this.defaultPrimaryTypeName = JSONHelper.valueFrom(json, "jcr:defaultPrimaryType");
    }

    protected Id id() {
        return id;
    }

    @Override
    public String getName() {
        return id.name;
    }

    @Override
    public boolean allowsSameNameSiblings() {
        return id.isMultiple;
    }

    @Override
    public String[] getRequiredPrimaryTypeNames() {
        return id.requiredTypes.toArray(new String[id.requiredTypes.size()]);
    }

    @Override
    public NodeType getDefaultPrimaryType() {
        return nodeTypes().getNodeType(getDefaultPrimaryTypeName());
    }

    @Override
    public String getDefaultPrimaryTypeName() {
        return defaultPrimaryTypeName;
    }

    @Override
    public javax.jcr.nodetype.NodeType[] getRequiredPrimaryTypes() {
        return nodeTypes().toNodeTypes(id.requiredTypes);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ChildNodeDefinition) {
            ChildNodeDefinition that = (ChildNodeDefinition)obj;
            return this.id.equals(that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" + ");
        sb.append(id.name);
        if (getRequiredPrimaryTypeNames().length != 0) {
            sb.append(" (");
            boolean first = true;
            for (String typeName : getRequiredPrimaryTypeNames()) {
                if (typeName == null) continue;
                if (first) first = false;
                else sb.append(',');
                sb.append(typeName);
            }
            sb.append(')');
        }
        if (getDefaultPrimaryTypeName() != null) {
            sb.append(" = ").append(getDefaultPrimaryTypeName());
        }
        if (isAutoCreated()) sb.append(" autocreated");
        if (isMandatory()) sb.append(" mandatory");
        if (allowsSameNameSiblings()) sb.append(" sns");
        if (isProtected()) sb.append(" protected");
        sb.append(' ').append(OnParentVersionAction.nameFromValue(getOnParentVersion()));
        return sb.toString();
    }

    protected static class Id {
        protected final String name;
        protected final boolean isMultiple;
        protected final Set<String> requiredTypes;

        protected Id( String name,
                      boolean isMultiple,
                      Set<String> requiredTypes ) {
            this.name = name;
            this.isMultiple = isMultiple;
            this.requiredTypes = requiredTypes;
            assert this.name != null;
            assert this.requiredTypes != null;
        }


        @Override
        public int hashCode() {
            return HashCode.compute(isMultiple, name, requiredTypes);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Id) {
                Id that = (Id)obj;
                if (this.isMultiple != that.isMultiple) return false;
                if (!this.requiredTypes.equals(that.requiredTypes)) return false;
                if (!this.name.equals(that.name)) return false;
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(name);
            sb.append('(');
            boolean first = true;
            for (String requiredType : requiredTypes) {
                if (first) first = false;
                else sb.append(',');
                sb.append(requiredType);
            }
            sb.append(')');
            sb.append(isMultiple ? '*' : '1');
            return sb.toString();
        }
    }
}
