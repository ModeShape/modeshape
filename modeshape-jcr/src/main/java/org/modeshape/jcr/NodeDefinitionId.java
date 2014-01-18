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

import java.util.HashMap;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * An immutable identifier for a node definition. Although instances cannot be serialized, the node definitions are often stored
 * within the graph as {@link #getString() string values} on a property. These string values can later be
 * {@link #fromString(String, NameFactory) parsed} to reconstruct the identifier. Note that this string representation does not
 * use namespace prefixes, so they are long-lasting and durable.
 * <p>
 * What distinguishes one property definition from another is not well documented in the JSR-170 specification. The closest this
 * version of the spec gets is Section 6.7.15, but that merely says that more than one property definition can have the same name.
 * The proposed draft of the JSR-283 specification does clarify this more: Section 4.7.15 says :
 * </p>
 * <p>
 * <quote>"Similarly, a node type may have two or more child node definitions with identical name attributes as long as they are
 * distinguishable by the required primary types attribute (the value returned by
 * NodeDefinition.getRequiredPrimaryTypes)."</quote>
 * </p>
 * <p>
 * This class is {@link Immutable} and designed to be used as a key in a {@link HashMap}.
 * </p>
 */
@Immutable
final class NodeDefinitionId {

    /**
     * The string-form of the name that can be used to represent a residual property definition.
     */
    public static final String ANY_NAME = JcrNodeType.RESIDUAL_ITEM_NAME;

    private final Name nodeTypeName;
    private final Name childDefinitionName;
    private final Name[] requiredPrimaryTypes;
    /**
     * A cached string representation, which is used for {@link #equals(Object)} and {@link #hashCode()} among other things.
     */
    private final String stringRepresentation;

    /**
     * Create an identifier for a node definition.
     * 
     * @param nodeTypeName the name of the node type on which this child node definition is defined; may not be null
     * @param childDefinitionName the name of the child node definition, which may be a {@link #ANY_NAME residual child
     *        definition}; may not be null
     * @param requiredPrimaryTypes the names of the required primary types for the child node definition
     */
    public NodeDefinitionId( Name nodeTypeName,
                             Name childDefinitionName,
                             Name[] requiredPrimaryTypes ) {
        assert nodeTypeName != null;
        assert childDefinitionName != null;
        this.nodeTypeName = nodeTypeName;
        this.childDefinitionName = childDefinitionName;
        this.requiredPrimaryTypes = requiredPrimaryTypes;
        StringBuilder sb = new StringBuilder(this.nodeTypeName.getString());
        sb.append('/').append(this.childDefinitionName.getString());
        for (Name requiredPrimaryType : requiredPrimaryTypes) {
            sb.append('/');
            sb.append(requiredPrimaryType.getString());
        }
        this.stringRepresentation = sb.toString();
    }

    /**
     * Get the name of the node type on which the child node definition is defined.
     * 
     * @return the node type's name; never null
     */
    public Name getNodeTypeName() {
        return nodeTypeName;
    }

    /**
     * Get the name of the child definition.
     * 
     * @return the child definition's name; never null
     */
    public Name getChildDefinitionName() {
        return childDefinitionName;
    }

    /**
     * @return requiredPrimaryTypes
     */
    public Name[] getRequiredPrimaryTypes() {
        Name[] copy = new Name[requiredPrimaryTypes.length];
        System.arraycopy(requiredPrimaryTypes, 0, copy, 0, requiredPrimaryTypes.length);
        return copy;
    }

    /**
     * Return whether there is at least one {@link #getRequiredPrimaryTypes() required primary type}
     * 
     * @return true if there is at least one required primary type, or false otherewise
     */
    public boolean hasRequiredPrimaryTypes() {
        return requiredPrimaryTypes.length != 0;
    }

    /**
     * Determine whether this node definition defines any named child.
     * 
     * @return true if this node definition allows children with any name, or false if this definition requires a particular child
     *         name
     */
    public boolean allowsAnyChildName() {
        return childDefinitionName.getLocalName().equals(ANY_NAME) && childDefinitionName.getNamespaceUri().length() == 0;
    }

    /**
     * Get the string form of this identifier. This form can be persisted, since it does not rely upon namespace prefixes.
     * 
     * @return the string form
     */
    public String getString() {
        return this.stringRepresentation;
    }

    /**
     * Parse the supplied string for of an identifer, and return the object form for that identifier.
     * 
     * @param definition the {@link #getString() string form of the identifier}; may not be null
     * @param factory the factory that should be used to create Name objects; may not be null
     * @return the object form of the identifier; never null
     * @throws ValueFormatException if the definition is not the valid format
     */
    public static NodeDefinitionId fromString( String definition,
                                               NameFactory factory ) {
        String[] parts = definition.split("/");
        String nodeTypeNameString = parts[0];
        String childDefinitionNameString = parts[1];
        Name[] requiredPrimaryTypes = new Name[parts.length - 2];
        for (int i = 2, j = 0; i != parts.length; ++i, ++j) {
            requiredPrimaryTypes[j] = factory.create(parts[i]);
        }
        Name nodeTypeName = factory.create(nodeTypeNameString);
        Name childDefinitionName = factory.create(childDefinitionNameString);
        return new NodeDefinitionId(nodeTypeName, childDefinitionName, requiredPrimaryTypes);
    }

    @Override
    public int hashCode() {
        return stringRepresentation.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof NodeDefinitionId) {
            NodeDefinitionId that = (NodeDefinitionId)obj;
            return this.stringRepresentation.equals(that.stringRepresentation);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.stringRepresentation;
    }

}
