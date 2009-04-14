/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;

/**
 * DNA implementation of the {@link NodeDefinition} class.
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
    private final Map<Name, JcrNodeType> requiredPrimaryTypesByName;

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
                       JcrNodeType[] requiredPrimaryTypes ) {
        this(context, null, declaringNodeType, name, onParentVersion, autoCreated, mandatory, protectedItem,
             allowsSameNameSiblings, defaultPrimaryTypeName, requiredPrimaryTypes);
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
                       JcrNodeType[] requiredPrimaryTypes ) {
        super(context, declaringNodeType, name, onParentVersion, autoCreated, mandatory, protectedItem);
        this.nodeTypeManager = nodeTypeManager;
        this.allowsSameNameSiblings = allowsSameNameSiblings;
        this.defaultPrimaryTypeName = defaultPrimaryTypeName;
        this.requiredPrimaryTypes = new JcrNodeType[requiredPrimaryTypes.length];
        this.requiredPrimaryTypeNames = new Name[requiredPrimaryTypes.length];
        for (int i = 0; i != requiredPrimaryTypes.length; ++i) {
            this.requiredPrimaryTypes[i] = requiredPrimaryTypes[i];
            this.requiredPrimaryTypeNames[i] = requiredPrimaryTypes[i].getInternalName();
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
        // Make a copy so that the caller can't modify our content ...
        NodeType[] result = new NodeType[requiredPrimaryTypes.length];
        for (int i = 0; i != requiredPrimaryTypes.length; ++i) {
            result[i] = requiredPrimaryTypes[i];
        }
        return result;
    }

    /**
     * Get the set of names of the primary types.
     * 
     * @return the required primary type names
     */
    Set<Name> getRequiredPrimaryTypeNames() {
        return requiredPrimaryTypesByName.keySet();
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
        JcrNodeType[] required = requiredPrimaryTypesByName.values().toArray(new JcrNodeType[requiredPrimaryTypesByName.size()]);
        return new JcrNodeDefinition(this.context, declaringNodeType.nodeTypeManager(), declaringNodeType, name,
                                     getOnParentVersion(), isAutoCreated(), isMandatory(), isProtected(),
                                     allowsSameNameSiblings(), defaultPrimaryTypeName, required);
    }

    JcrNodeDefinition with( ExecutionContext context ) {
        JcrNodeType[] required = requiredPrimaryTypesByName.values().toArray(new JcrNodeType[requiredPrimaryTypesByName.size()]);
        return new JcrNodeDefinition(context, this.nodeTypeManager, this.declaringNodeType, name, getOnParentVersion(),
                                     isAutoCreated(), isMandatory(), isProtected(), allowsSameNameSiblings(),
                                     defaultPrimaryTypeName, required);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getId().toString();
    }

    Key getKey(boolean includeTypes) {
        return new Key(this, includeTypes);
    }

    /**
     * Internal class that encapsulates composite unique identifier for child node definitions
     */
    class Key {
        String keyString;

        Key( Node node,
             NamespaceRegistry registry ) throws Exception {
            StringBuffer buff = new StringBuffer();

            try {
                String nodeName = node.getProperty(JcrLexicon.NAME.getString(registry)).getString();
                buff.append(nodeName);
            } catch (PathNotFoundException pnfe) {
                // Ignorable, means name is not set.
                buff.append(JcrNodeType.RESIDUAL_ITEM_NAME);
            }

            try {
                Value[] requiredTypes = node.getProperty(JcrLexicon.REQUIRED_PRIMARY_TYPES.getString(registry)).getValues();

                for (int i = 0; i < requiredTypes.length; i++) {
                    buff.append('_').append(requiredTypes[i].getString());
                }
            } catch (PathNotFoundException pnfe) {
                // No required types. Weird, but not debilitating.
            }

            buff.append('_').append(node.getProperty(JcrLexicon.SAME_NAME_SIBLINGS.getString(registry)).getBoolean());

            this.keyString = buff.toString();
        }

        Key( NodeDefinition def,
             boolean includeTypes ) {
            StringBuffer buff = new StringBuffer();
            buff.append(def.getName());

            if (includeTypes) {
                NodeType[] requiredTypes = def.getRequiredPrimaryTypes();
                for (int i = 0; i < requiredTypes.length; i++) {
                    buff.append('_').append(requiredTypes[i].getName());
                }
            }

            buff.append('_').append(def.allowsSameNameSiblings());

            this.keyString = buff.toString();
        }

        @Override
        public boolean equals( Object ob ) {
            if (!(ob instanceof Key)) {
                return false;
            }

            return keyString.equals(((Key)ob).keyString);
        }

        @Override
        public int hashCode() {
            return keyString.hashCode();
        }
    }

}
