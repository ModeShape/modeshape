/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.io.Serializable;
import java.util.HashMap;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.ValueFormatException;

/**
 * An immutable identifier for a node definition. Although instances can be serialized, the node definitions are often stored
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
 * This class is {@link Serializable} and designed to be used as a key in a {@link HashMap}.
 * </p>
 */
@Immutable
final class NodeDefinitionId implements Serializable {

    /**
     * Current version is {@value} .
     */
    private static final long serialVersionUID = 1L;

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

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return stringRepresentation.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof NodeDefinitionId) {
            NodeDefinitionId that = (NodeDefinitionId)obj;
            return this.stringRepresentation.equals(that.stringRepresentation);
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
        return this.stringRepresentation;
    }

}
