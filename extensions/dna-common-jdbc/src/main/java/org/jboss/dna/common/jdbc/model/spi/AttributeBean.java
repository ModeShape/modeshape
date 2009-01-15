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
package org.jboss.dna.common.jdbc.model.spi;

import org.jboss.dna.common.jdbc.model.api.Attribute;
import org.jboss.dna.common.jdbc.model.api.Reference;

/**
 * Provides User Defined Type (UDT) attribute specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 * @since 1.4 (JDBC 3.0)
 */
public class AttributeBean extends ColumnBean implements Attribute {
    private static final long serialVersionUID = -2822658260714209072L;
    private Reference reference;

    /**
     * Default constructor
     */
    public AttributeBean() {
    }

    /**
     * Returns column reference if datatype is REF
     * 
     * @return column reference if datatype is REF
     */
    public Reference getReference() {
        return reference;
    }

    /**
     * Sets reference
     * 
     * @param reference the reference to set
     */
    public void setReference( Reference reference ) {
        this.reference = reference;
    }
}
