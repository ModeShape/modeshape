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

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.Name;

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
    private final NodeType[] requiredPrimaryTypes;

    /** A durable identifier for this node definition. */
    private NodeDefinitionId id;

    JcrNodeDefinition( JcrSession session,
                       JcrNodeType declaringNodeType,
                       Name name,
                       int onParentVersion,
                       boolean autoCreated,
                       boolean mandatory,
                       boolean protectedItem,
                       boolean allowsSameNameSiblings,
                       Name defaultPrimaryTypeName,
                       NodeType[] requiredPrimaryTypes ) {
        super(session, declaringNodeType, name, onParentVersion, autoCreated, mandatory, protectedItem);
        this.allowsSameNameSiblings = allowsSameNameSiblings;
        this.defaultPrimaryTypeName = defaultPrimaryTypeName;
        this.requiredPrimaryTypes = requiredPrimaryTypes;
    }

    /**
     * Get the durable identifier for this node definition.
     * 
     * @return the node definition ID; never null
     */
    public NodeDefinitionId getId() {
        if (id == null) {
            id = new NodeDefinitionId(declaringNodeType.getInternalName(), name);
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

        /*
         * Translate the name to a prefixed type based on the current transient (session) and persistent (workspace) 
         * prefix to URI mappings.
         */
        String mappedTypeName = defaultPrimaryTypeName.getString(session.getExecutionContext().getNamespaceRegistry());

        try {
            return session.getWorkspace().getNodeTypeManager().getNodeType(mappedTypeName);
        } catch (RepositoryException re) {
            /*
             * The spec doesn't allow us to throw a checked exception at this point, but a corrupted namespace mapping
             * would be pretty severe. 
             */
            throw new IllegalStateException(JcrI18n.typeNotFound.text(mappedTypeName));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#getRequiredPrimaryTypes()
     */
    public NodeType[] getRequiredPrimaryTypes() {
        return requiredPrimaryTypes;
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
        return new JcrNodeDefinition(session, declaringNodeType, name, getOnParentVersion(), isAutoCreated(), isMandatory(),
                                     isProtected(), allowsSameNameSiblings(), defaultPrimaryTypeName, requiredPrimaryTypes);
    }
}
