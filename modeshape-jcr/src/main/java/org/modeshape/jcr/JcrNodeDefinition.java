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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.RepositoryNodeTypeManager.NodeTypes;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.ValueFactory;

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

    private final Name[] requiredPrimaryTypeNames;
    private final Set<Name> requiredPrimaryTypeNameSet;

    /** A durable identifier for this node definition. */
    private final NodeDefinitionId id;

    private final NodeKey key;

    /** Link to the repository node type manager */
    private final RepositoryNodeTypeManager nodeTypeManager;

    JcrNodeDefinition( ExecutionContext context,
                       JcrNodeType declaringNodeType,
                       NodeKey prototypeKey,
                       Name name,
                       int onParentVersion,
                       boolean autoCreated,
                       boolean mandatory,
                       boolean protectedItem,
                       boolean allowsSameNameSiblings,
                       Name defaultPrimaryTypeName,
                       Name[] requiredPrimaryTypeNames ) {
        this(context, null, declaringNodeType, prototypeKey, name, onParentVersion, autoCreated, mandatory, protectedItem,
             allowsSameNameSiblings, defaultPrimaryTypeName, requiredPrimaryTypeNames);
    }

    JcrNodeDefinition( ExecutionContext context,
                       RepositoryNodeTypeManager nodeTypeManager,
                       JcrNodeType declaringNodeType,
                       NodeKey prototypeKey,
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
        this.requiredPrimaryTypeNames = requiredPrimaryTypeNames;
        this.id = this.declaringNodeType == null ? null : new NodeDefinitionId(this.declaringNodeType.getInternalName(),
                                                                               this.name, this.requiredPrimaryTypeNames);
        this.key = this.id == null ? prototypeKey : prototypeKey.withId("/jcr:system/jcr:nodeTypes/" + this.id.getString());

        // Cache the set of required primary type names ...
        Set<Name> requiredPrimaryTypeNameSet = new HashSet<Name>();
        for (Name requiredTypeName : this.requiredPrimaryTypeNames) {
            requiredPrimaryTypeNameSet.add(requiredTypeName);
        }
        this.requiredPrimaryTypeNameSet = Collections.unmodifiableSet(requiredPrimaryTypeNameSet);
    }

    private final String string( Name name ) {
        if (name == null) return null;
        if (this.context == null) return name.getString();

        return name.getString(context.getNamespaceRegistry());
    }

    @Override
    final NodeKey key() {
        return key;
    }

    /**
     * Get the durable identifier for this node definition.
     * 
     * @return the node definition ID; never null
     */
    public NodeDefinitionId getId() {
        return id;
    }

    @Override
    public boolean allowsSameNameSiblings() {
        return allowsSameNameSiblings;
    }

    /**
     * Get whether this child node definition has restrictions on the primary type of the children.
     * 
     * @return true if there is 1 or more required primary types, or false if there are none
     */
    public boolean hasRequiredPrimaryTypes() {
        return id.hasRequiredPrimaryTypes();
    }

    /**
     * @return the name of the default primary type for this definition; may be null
     */
    final Name defaultPrimaryTypeName() {
        return defaultPrimaryTypeName;
    }

    @Override
    public JcrNodeType getDefaultPrimaryType() {
        // It is valid for this field to be null.
        if (defaultPrimaryTypeName == null) {
            return null;
        }

        return nodeTypeManager.getNodeTypes().getNodeType(defaultPrimaryTypeName);
    }

    @Override
    public NodeType[] getRequiredPrimaryTypes() {
        if (requiredPrimaryTypeNames.length == 0) {
            // Per the JavaDoc, this method should never return null or an empty array; if there are no constraints,
            // then this method should include an array with 'nt:base' as the required primary type.
            NodeType[] result = new NodeType[1];
            result[0] = nodeTypeManager.getNodeTypes().getNodeType(JcrNtLexicon.BASE);
            return result;
        }
        // Make a copy so that the caller can't modify our content ...
        NodeType[] result = new NodeType[requiredPrimaryTypeNames.length];
        int i = 0;
        final NodeTypes nodeTypes = nodeTypeManager.getNodeTypes();
        for (Name name : requiredPrimaryTypeNames) {
            result[i++] = nodeTypes.getNodeType(name);
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
        return requiredPrimaryTypeNameSet;
    }

    @Override
    public String[] getRequiredPrimaryTypeNames() {
        if (requiredPrimaryTypeNames == null) return new String[0];

        String[] rptNames = new String[requiredPrimaryTypeNames.length];
        for (int i = 0; i < requiredPrimaryTypeNames.length; i++) {
            rptNames[i] = string(requiredPrimaryTypeNames[i]);
        }
        return rptNames;
    }

    @Override
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
        // The supplied primary type must be or extend all of the required primary types ...
        for (Name requiredPrimaryTypeName : requiredPrimaryTypeNameSet) {
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
        return new JcrNodeDefinition(this.context, declaringNodeType.nodeTypeManager(), declaringNodeType, key(), name,
                                     getOnParentVersion(), isAutoCreated(), isMandatory(), isProtected(),
                                     allowsSameNameSiblings(), defaultPrimaryTypeName, requiredPrimaryTypeNames);
    }

    JcrNodeDefinition with( ExecutionContext context ) {
        return new JcrNodeDefinition(context, this.nodeTypeManager, this.declaringNodeType, key(), name, getOnParentVersion(),
                                     isAutoCreated(), isMandatory(), isProtected(), allowsSameNameSiblings(),
                                     defaultPrimaryTypeName, requiredPrimaryTypeNames);
    }

    JcrNodeDefinition with( RepositoryNodeTypeManager nodeTypeManager ) {
        return new JcrNodeDefinition(this.context, nodeTypeManager, this.declaringNodeType, key(), name, getOnParentVersion(),
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
