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
package org.modeshape.jcr.nodetype;

import java.util.List;
import net.jcip.annotations.NotThreadSafe;

/**
 * A template that can be used to create new node types, patterned after the approach in the proposed <a
 * href="http://jcp.org/en/jsr/detail?id=283">JSR-283</a>. This interface extends the {@link NodeTypeDefinition} interface and
 * adds setter methods for the various attributes.
 */
@NotThreadSafe
public interface NodeTypeTemplate extends NodeTypeDefinition {

    /**
     * Set the name of the node type
     * 
     * @param name the name
     */
    public void setName( String name );

    /**
     * Set the direct supertypes for this node type.
     * 
     * @param names the names of the direct supertypes, or empty or null if there are none.
     */
    public void setDeclaredSupertypeNames( String[] names );

    /**
     * Set whether this node type is abstract.
     * 
     * @param isAbstract true if this node type is to be abstract, or false if it is concrete
     */
    public void setAbstract( boolean isAbstract );

    /**
     * Sets whether this node is queryable
     * 
     * @param queryable true if the node should be included in query results; false otherwise
     */
    public void setQueryable( boolean queryable );

    /**
     * Set whether this node type is a mixin.
     * 
     * @param mixin true if this node type is a mixin, or false otherwise
     */
    public void setMixin( boolean mixin );

    /**
     * Set whether this node type supports orderable child nodes.
     * 
     * @param orderable true if this node type supports orderable child nodes, or false otherwise
     */
    public void setOrderableChildNodes( boolean orderable );

    /**
     * Set the name of the primary item for this node type
     * 
     * @param name the name of the child node or property that represents the primary item for nodes that use this type, or null
     *        if there is none
     */
    public void setPrimaryItemName( String name );

    /**
     * Get the modifiable list of property definition templates for this node type.
     * 
     * @return the node type's list of property definition templates; never null
     */
    public List<PropertyDefinitionTemplate> getPropertyDefinitionTemplates();

    /**
     * Get the modifiable list of child node definition templates for this node type
     * 
     * @return the node type's list of child node definition templates; never null
     */
    public List<NodeDefinitionTemplate> getNodeDefinitionTemplates();

}
