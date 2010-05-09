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

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.NotThreadSafe;

/**
 * An interface for an existing node type definition, patterned after the approach in the proposed <a
 * href="http://jcp.org/en/jsr/detail?id=283">JSR-283</a>.
 */
@NotThreadSafe
public interface NodeTypeDefinition {

    /**
     * Get the name of the node type being defined
     * 
     * @return the name
     */
    public String getName();

    /**
     * Get the direct supertypes for this node type.
     * 
     * @return the names of the direct supertypes, or an empty array if there are none
     */
    public String[] getDeclaredSupertypes();

    /**
     * Get the direct supertypes for this node type.
     * 
     * @return the names of the direct supertypes, or an empty array if there are none
     */
    public String[] getDeclaredSupertypeNames();

    /**
     * Get whether this node type is abstract.
     * 
     * @return true if this node type is abstract, or false if it is concrete
     */
    public boolean isAbstract();

    /**
     * Get whether this node type is abstract.
     * 
     * @return true if this node type is abstract, or false if it is concrete
     */
    public boolean isQueryable();

    /**
     * Get whether this node type is a mixin.
     * 
     * @return true if this node type is a mixin, or false if it is concrete
     */
    public boolean isMixin();

    /**
     * Get whether this node type supports orderable child nodes.
     * 
     * @return true if this node type supports orderable child nodes, or false otherwise
     */
    public boolean hasOrderableChildNodes();

    /**
     * Get the name of the primary item for this node type
     * 
     * @return the name of the child node or property that represents the primary item for nodes that use this type, or null if
     *         there is none
     */
    public String getPrimaryItemName();

    /**
     * Get the array of property definition templates for this node type.
     * 
     * @return the node type's list of property definitions; never null
     */
    public PropertyDefinition[] getDeclaredPropertyDefinitions();

    /**
     * Get the array of child node definition templates for this node type
     * 
     * @return the node type's list of child node definitions; never null
     */

    public NodeDefinition[] getDeclaredNodeDefinitions();
}
