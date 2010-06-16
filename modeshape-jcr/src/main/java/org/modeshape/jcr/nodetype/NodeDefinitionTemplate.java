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

import javax.jcr.nodetype.ConstraintViolationException;
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
     * Set the names of the primary types that must appear on the child(ren) described by this definition
     * 
     * @param requiredPrimaryTypes the names of the required primary types, or null or empty if there are no requirements for the
     *        primary types of the children described by this definition
     * @throws ConstraintViolationException if any of the <code>requiredPrimaryTypes</code> are not a syntactically valid JCR name
     *         in either qualified or expanded form.
     * @deprecated As of ModeShape 2.0, use {@link #setRequiredPrimaryTypeNames(String[])} instead
     */
    @Deprecated
    public void setRequiredPrimaryTypes( String[] requiredPrimaryTypes ) throws ConstraintViolationException;

    /**
     * Set the name of the primary type that should be used by default when creating children using this node definition.
     * 
     * @param defaultPrimaryType the name of the primary type that should be used by default, or null if there is none
     * @throws ConstraintViolationException if <code>defaultPrimaryType</code> is not a syntactically valid JCR name in either
     *         qualified or expanded form.
     * @deprecated As of ModeShape 2.0, use {@link #setDefaultPrimaryTypeName(String)} instead
     */
    @Deprecated
    public void setDefaultPrimaryType( String defaultPrimaryType ) throws ConstraintViolationException;

}
