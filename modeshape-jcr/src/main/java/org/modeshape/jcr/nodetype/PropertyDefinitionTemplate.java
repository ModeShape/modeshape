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

import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.NotThreadSafe;

/**
 * A template that can be used to create new property definitions, patterned after the approach in the proposed <a
 * href="http://jcp.org/en/jsr/detail?id=283">JSR-283</a>. This interface extends the standard {@link PropertyDefinition}
 * interface and adds setter methods for the various attributes.
 * 
 * @see NodeTypeTemplate#getDeclaredPropertyDefinitions()
 */
@NotThreadSafe
public interface PropertyDefinitionTemplate extends javax.jcr.nodetype.PropertyDefinitionTemplate {

    /**
     * Set the default values for the property, using their string representation. See
     * {@link PropertyDefinition#getDefaultValues()} for more details.
     * 
     * @param defaultValues the string representation of the default values, or null or an empty array if there are no default
     *        values
     * @deprecated As of ModeShape 2.0, use {@link #setDefaultValues(javax.jcr.Value[])} instead
     */
    @Deprecated
    public void setDefaultValues( String[] defaultValues );

}
