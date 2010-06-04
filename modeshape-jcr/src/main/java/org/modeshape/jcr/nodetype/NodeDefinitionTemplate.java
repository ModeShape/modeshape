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

import javax.jcr.Node;
import javax.jcr.version.OnParentVersionAction;
import net.jcip.annotations.NotThreadSafe;

/**
 * A template that can be used to create new child node definitions, patterned after the approach in the proposed <a
 * href="http://jcp.org/en/jsr/detail?id=283">JSR-283</a>. This interface extends the standard {@link NodeTypeDefinition}
 * interface and adds setter methods for the various attributes.
 * 
 * @see javax.jcr.nodetype.NodeTypeTemplate#getNodeDefinitionTemplates()
 */
@NotThreadSafe
public interface NodeDefinitionTemplate extends javax.jcr.nodetype.NodeDefinitionTemplate {

    /**
     * Set the name of this child node definition.
     * 
     * @param name the name for this child node definition.
     */
    public void setName( String name );

    /**
     * Set whether this definition describes a child node that is auto-created by the system.
     * 
     * @param autoCreated true if this child should be auto-created
     */
    public void setAutoCreated( boolean autoCreated );

    /**
     * Set whether this definition describes a child that is required (mandatory).
     * 
     * @param mandatory true if the child is mandatory
     */
    public void setMandatory( boolean mandatory );

    /**
     * Set the mode for the versioning of the child with respect to versioning of the parent.
     * 
     * @param opv the on-parent versioning mode; one of {@link OnParentVersionAction} values.
     */
    public void setOnParentVersion( int opv );

    /**
     * Set whether the child node described by this definition is protected from changes through the JCR API.
     * 
     * @param isProtected true if the child node is protected, or false if it may be changed through the JCR API
     */
    public void setProtected( boolean isProtected );

    /**
     * Set the names of the primary types that must appear on the child(ren) described by this definition
     * 
     * @param requiredPrimaryTypes the names of the required primary types, or null or empty if there are no requirements for the
     *        primary types of the children described by this definition
     */
    public void setRequiredPrimaryTypes( String[] requiredPrimaryTypes );

    /**
     * Set the name of the primary type that should be used by default when creating children using this node definition.
     * 
     * @param defaultPrimaryType the name of the primary type that should be used by default, or null if there is none
     */
    public void setDefaultPrimaryType( String defaultPrimaryType );

    /**
     * Set whether the children described by this definition may have the same names (and therefore distinguished only by their
     * {@link Node#getIndex() same-name-sibiling index}).
     * 
     * @param allowSameNameSiblings true if the children described by this definition may have the same names, or false otherwise
     */
    public void setSameNameSiblings( boolean allowSameNameSiblings );

}
