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

import java.util.Collection;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * An immutable representation of an abstract JCR ItemDefinition.
 */
@Immutable
public abstract class ItemDefinition implements javax.jcr.nodetype.ItemDefinition {

    protected static String[] toArray( Collection<String> values ) {
        if (values == null) return null;
        if (values.isEmpty()) return new String[0];
        return values.toArray(new String[values.size()]);
    }

    protected static javax.jcr.nodetype.NodeType[] nodeTypes( Collection<String> nodeTypeNames,
                                                              Map<String, NodeType> nodeTypes ) {
        if (nodeTypes == null || nodeTypeNames == null || nodeTypeNames.isEmpty()) return new javax.jcr.nodetype.NodeType[0];
        int numValues = nodeTypeNames.size();
        int i = 0;
        NodeType[] result = new NodeType[numValues];
        for (String requiredTypeName : nodeTypeNames) {
            result[i++] = nodeTypes.get(requiredTypeName);
        }
        return result;
    }

    private final String declaringNodeTypeName;
    private final boolean isAutoCreated;
    private final boolean isMandatory;
    private final boolean isProtected;
    private final int onParentVersion;
    private final Map<String, NodeType> nodeTypes;

    protected ItemDefinition( String declaringNodeTypeName,
                              boolean isAutoCreated,
                              boolean isMandatory,
                              boolean isProtected,
                              int onParentVersion,
                              Map<String, NodeType> nodeTypes ) {
        assert declaringNodeTypeName != null;
        this.declaringNodeTypeName = declaringNodeTypeName;
        this.isAutoCreated = isAutoCreated;
        this.isMandatory = isMandatory;
        this.isProtected = isProtected;
        this.onParentVersion = onParentVersion;
        this.nodeTypes = nodeTypes;
    }

    /**
     * Find the node type with the supplied name.
     * 
     * @param name the name of the node type to find.
     * @return the named node type, or null if the node type is not known (or there are no node types known)
     */
    protected NodeType nodeType( String name ) {
        return nodeTypes != null ? nodeTypes.get(name) : null;
    }

    /**
     * @return nodeTypes
     */
    protected Map<String, NodeType> nodeTypes() {
        return nodeTypes;
    }

    /**
     * Get the name of the node type that declares this definition.
     * 
     * @return the node type name; never null
     */
    public String getDeclaringNodeTypeName() {
        return declaringNodeTypeName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#getDeclaringNodeType()
     */
    @Override
    public NodeType getDeclaringNodeType() {
        return nodeType(declaringNodeTypeName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#getOnParentVersion()
     */
    @Override
    public int getOnParentVersion() {
        return onParentVersion;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#isAutoCreated()
     */
    @Override
    public boolean isAutoCreated() {
        return isAutoCreated;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#isMandatory()
     */
    @Override
    public boolean isMandatory() {
        return isMandatory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#isProtected()
     */
    @Override
    public boolean isProtected() {
        return isProtected;
    }

}
