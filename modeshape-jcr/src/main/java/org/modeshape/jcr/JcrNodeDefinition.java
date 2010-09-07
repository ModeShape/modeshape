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
package org.modeshape.jcr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.ValueFactory;

/**
 * ModeShape implementation of the {@link NodeDefinition} class.
 */
@Immutable
class JcrNodeDefinition extends JcrItemDefinition implements NodeDefinition {

    /** @see NodeDefinition#allowsSameNameSiblings() */
    private final boolean allowsSameNameSiblings;

    /**
     * The name of the default primary type (if any). The name is used instead of the raw node type to allow circular references a
     * la <code>nt:unstructured</code>.
     */
    private final Name defaultPrimaryTypeName;

    /** @see NodeDefinition#getRequiredPrimaryTypes() */
    private Map<Name, JcrNodeType> requiredPrimaryTypesByName;

    private JcrNodeType[] requiredPrimaryTypes;

    private Name[] requiredPrimaryTypeNames;

    /** A durable identifier for this node definition. */
    private NodeDefinitionId id;

    /** Link to the repository node type manager */
    private final RepositoryNodeTypeManager nodeTypeManager;

    JcrNodeDefinition( ExecutionContext context,
                       JcrNodeType declaringNodeType,
                       Name name,
                       int onParentVersion,
                       boolean autoCreated,
                       boolean mandatory,
                       boolean protectedItem,
                       boolean allowsSameNameSiblings,
                       Name defaultPrimaryTypeName,
                       Name[] requiredPrimaryTypeNames ) {
        this(context, null, declaringNodeType, name, onParentVersion, autoCreated, mandatory, protectedItem,
             allowsSameNameSiblings, defaultPrimaryTypeName, requiredPrimaryTypeNames);
    }

    JcrNodeDefinition( ExecutionContext context,
                       RepositoryNodeTypeManager nodeTypeManager,
                       JcrNodeType declaringNodeType,
                       Name name,
                       int onParentVersion,
                       boolean autoCreated,
                       boolean mandatory,
                       boolean protectedItem,
                       boolean allowsSameNameSiblings,
                       Name defaultPrimaryTypeName,
                       Name[] requiredPrimaryTypeNames ) {
        super(context, declaringNodeType, name, onParentVersion, autoCreated, mandatory, protectedItem);
        this.nodeTypeManager = nodeTypeManager;
        this.allowsSameNameSiblings = allowsSameNameSiblings;
        this.defaultPrimaryTypeName = defaultPrimaryTypeName;
        this.requiredPrimaryTypes = new JcrNodeType[requiredPrimaryTypeNames.length];
        this.requiredPrimaryTypeNames = requiredPrimaryTypeNames;
    }

    private final String string( Name name ) {
        if (name == null) return null;
        if (this.context == null) return name.getString();

        return name.getString(context.getNamespaceRegistry());
    }

    /**
     * Checks that the fields derived from requiredPrimaryTypeNames are initialized.
     * <p>
     * This was pulled out of the constructor to make type registration more flexible by deferring node type lookup for required
     * primary types until after type registration is complete. This allows, for example, nodes to have themselves as required
     * primary types of their children.
     * </p>
     */
    private void ensureRequiredPrimaryTypesLoaded() {
        if (requiredPrimaryTypesByName != null) return;
        this.requiredPrimaryTypes = new JcrNodeType[requiredPrimaryTypeNames.length];
        for (int i = 0; i != requiredPrimaryTypeNames.length; ++i) {
            this.requiredPrimaryTypes[i] = nodeTypeManager.getNodeType(requiredPrimaryTypeNames[i]);
        }
        Map<Name, JcrNodeType> requiredPrimaryTypesByName = new HashMap<Name, JcrNodeType>();
        for (JcrNodeType requiredPrimaryType : requiredPrimaryTypes) {
            requiredPrimaryTypesByName.put(requiredPrimaryType.getInternalName(), requiredPrimaryType);
        }
        this.requiredPrimaryTypesByName = Collections.unmodifiableMap(requiredPrimaryTypesByName);

    }

    /**
     * Get the durable identifier for this node definition.
     * 
     * @return the node definition ID; never null
     */
    public NodeDefinitionId getId() {
        if (id == null) {
            // This is idempotent, so no need to lock
            id = new NodeDefinitionId(this.declaringNodeType.getInternalName(), this.name, this.requiredPrimaryTypeNames);
        }
        return id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#allowsSameNameSiblings()
     */
    public boolean allowsSameNameSiblings() {
        return allowsSameNameSiblings;
    }

    /**
     * @return the name of the default primary type for this definition; may be null
     */
    final Name defaultPrimaryTypeName() {
        return defaultPrimaryTypeName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#getDefaultPrimaryType()
     */
    public NodeType getDefaultPrimaryType() {
        // It is valid for this field to be null.
        if (defaultPrimaryTypeName == null) {
            return null;
        }

        return nodeTypeManager.getNodeType(defaultPrimaryTypeName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#getRequiredPrimaryTypes()
     */
    public NodeType[] getRequiredPrimaryTypes() {
        ensureRequiredPrimaryTypesLoaded();
        if (requiredPrimaryTypes.length == 0) {
            // Per the JavaDoc, this method should never return null or an empty array; if there are no constraints,
            // then this method should include an array with 'nt:base' as the required primary type.
            NodeType[] result = new NodeType[1];
            result[0] = nodeTypeManager.getNodeType(JcrNtLexicon.BASE);
            return result;
        }
        // Make a copy so that the caller can't modify our content ...
        NodeType[] result = new NodeType[requiredPrimaryTypes.length];
        for (int i = 0; i != requiredPrimaryTypes.length; ++i) {
            result[i] = requiredPrimaryTypes[i];
        }
        return result;
    }

    /**
     * Returns the required primary type names for this object as specified in the constructor. This method is useful for callers
     * that wish to access this information while this node definition's parent node is being registered.
     * 
     * @return the required primary type names
     */
    Name[] requiredPrimaryTypeNames() {
        return this.requiredPrimaryTypeNames;
    }

    /**
     * Get the set of names of the primary types.
     * 
     * @return the required primary type names
     */
    Set<Name> requiredPrimaryTypeNameSet() {
        ensureRequiredPrimaryTypesLoaded();
        return requiredPrimaryTypesByName.keySet();
    }

    /**
     * @return the names of the required primary types for this child node definition; never null
     */
    public String[] getRequiredPrimaryTypeNames() {
        if (requiredPrimaryTypeNames == null) return new String[0];

        String[] rptNames = new String[requiredPrimaryTypeNames.length];
        for (int i = 0; i < requiredPrimaryTypeNames.length; i++) {
            rptNames[i] = string(requiredPrimaryTypeNames[i]);
        }
        return rptNames;
    }

    /**
     * @return the name of the default primary type for this child node definition; may be null
     */
    public String getDefaultPrimaryTypeName() {
        return string(this.defaultPrimaryTypeName);
    }

    /**
     * Determine if this node definition will allow a child with the supplied primary type. This method checks this definition's
     * {@link #getRequiredPrimaryTypes()} against the supplied primary type and its supertypes. The supplied primary type for the
     * child must be or extend all of the types defined by the {@link #getRequiredPrimaryTypes() required primary types}.
     * 
     * @param childPrimaryType the primary type of the child
     * @return true if the primary type of the child (or one of its supertypes) is one of the types required by this definition,
     *         or false otherwise
     */
    final boolean allowsChildWithType( JcrNodeType childPrimaryType ) {
        if (childPrimaryType == null) {
            // The definition must have a default primary type ...
            if (defaultPrimaryTypeName != null) {
                return true;
            }
            return false;
        }
        ensureRequiredPrimaryTypesLoaded();
        // The supplied primary type must be or extend all of the required primary types ...
        for (Name requiredPrimaryTypeName : requiredPrimaryTypesByName.keySet()) {
            if (!childPrimaryType.isNodeType(requiredPrimaryTypeName)) return false;
        }
        return true;
    }

    /**
     * Creates a new <code>JcrNodeDefinition</code> that is identical to the current object, but with the given
     * <code>declaringNodeType</code>. Provided to support immutable pattern for this class.
     * 
     * @param declaringNodeType the declaring node type for the new <code>JcrNodeDefinition</code>
     * @return a new <code>JcrNodeDefinition</code> that is identical to the current object, but with the given
     *         <code>declaringNodeType</code>.
     */
    JcrNodeDefinition with( JcrNodeType declaringNodeType ) {
        return new JcrNodeDefinition(this.context, declaringNodeType.nodeTypeManager(), declaringNodeType, name,
                                     getOnParentVersion(), isAutoCreated(), isMandatory(), isProtected(),
                                     allowsSameNameSiblings(), defaultPrimaryTypeName, requiredPrimaryTypeNames);
    }

    JcrNodeDefinition with( ExecutionContext context ) {
        return new JcrNodeDefinition(context, this.nodeTypeManager, this.declaringNodeType, name, getOnParentVersion(),
                                     isAutoCreated(), isMandatory(), isProtected(), allowsSameNameSiblings(),
                                     defaultPrimaryTypeName, requiredPrimaryTypeNames);
    }

    JcrNodeDefinition with( RepositoryNodeTypeManager nodeTypeManager ) {
        return new JcrNodeDefinition(this.context, nodeTypeManager, this.declaringNodeType, name, getOnParentVersion(),
                                     isAutoCreated(), isMandatory(), isProtected(), allowsSameNameSiblings(),
                                     defaultPrimaryTypeName, requiredPrimaryTypeNames);
    }

    @Override
    public int hashCode() {
        return getId().toString().hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        JcrNodeDefinition other = (JcrNodeDefinition)obj;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        ValueFactory<String> strings = context.getValueFactories().getStringFactory();
        StringBuilder sb = new StringBuilder();
        NodeDefinitionId id = getId();
        sb.append(strings.create(id.getNodeTypeName()));
        sb.append('/');
        sb.append(strings.create(id.getChildDefinitionName()));
        if (id.hasRequiredPrimaryTypes()) {
            sb.append(" (required primary types = [");
            boolean first = true;
            for (Name requiredPrimaryType : id.getRequiredPrimaryTypes()) {
                if (first) first = false;
                else sb.append(',');
                sb.append(requiredPrimaryType.getString());
            }
            sb.append("])");
        }
        return sb.toString();
    }
}
