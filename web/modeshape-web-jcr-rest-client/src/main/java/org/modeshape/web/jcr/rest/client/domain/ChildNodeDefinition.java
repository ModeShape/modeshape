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
package org.modeshape.web.jcr.rest.client.domain;

import java.util.Map;
import java.util.Set;
import javax.jcr.version.OnParentVersionAction;
import net.jcip.annotations.Immutable;

/**
 * An immutable representation of a JCR PropertyDefinition.
 */
@Immutable
public class ChildNodeDefinition extends ItemDefinition implements javax.jcr.nodetype.NodeDefinition {

    private final Id id;
    private final String defaultPrimaryTypeName;

    public ChildNodeDefinition( String declaringNodeTypeName,
                                String name,
                                Set<String> requiredTypes,
                                boolean isAutoCreated,
                                boolean isMandatory,
                                boolean isProtected,
                                boolean allowsSameNameSiblings,
                                int onParentVersion,
                                String defaultPrimaryTypeName,
                                Map<String, NodeType> nodeTypes ) {
        super(declaringNodeTypeName, isAutoCreated, isMandatory, isProtected, onParentVersion, nodeTypes);
        this.id = new Id(name, allowsSameNameSiblings, requiredTypes);
        this.defaultPrimaryTypeName = defaultPrimaryTypeName;
    }

    /**
     * @return id
     */
    protected Id id() {
        return id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#getName()
     */
    @Override
    public String getName() {
        return id.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#allowsSameNameSiblings()
     */
    @Override
    public boolean allowsSameNameSiblings() {
        return id.isMultiple;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#getRequiredPrimaryTypeNames()
     */
    @Override
    public String[] getRequiredPrimaryTypeNames() {
        return toArray(id.requiredTypes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#getDefaultPrimaryType()
     */
    @Override
    public NodeType getDefaultPrimaryType() {
        return nodeType(getDefaultPrimaryTypeName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#getDefaultPrimaryTypeName()
     */
    @Override
    public String getDefaultPrimaryTypeName() {
        return defaultPrimaryTypeName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#getRequiredPrimaryTypes()
     */
    @Override
    public javax.jcr.nodetype.NodeType[] getRequiredPrimaryTypes() {
        return nodeTypes(id.requiredTypes, nodeTypes());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ChildNodeDefinition) {
            ChildNodeDefinition that = (ChildNodeDefinition)obj;
            return this.id.equals(that.id);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return name.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
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

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
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
