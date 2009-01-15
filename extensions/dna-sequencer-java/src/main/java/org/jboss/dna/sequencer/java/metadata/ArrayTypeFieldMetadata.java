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
package org.jboss.dna.sequencer.java.metadata;


/**
 * ArrayTypeFieldMetadata represents the meta data for an array type.
 * 
 * @author Serge Pagop
 */
public class ArrayTypeFieldMetadata extends FieldMetadata {
   
    /**
     * {@inheritDoc}
     *
     * @see org.jboss.dna.sequencer.java.metadata.FieldMetadata#isArrayType()
     */
    @Override
    public boolean isArrayType() {
        return true;
    }

    // Element type 
    private FieldMetadata elementType;
    
    // Component type
    private FieldMetadata componentType;

    public ArrayTypeFieldMetadata() {
        
    }

    /**
     * @return elementType
     */
    public FieldMetadata getElementType() {
        return elementType;
    }

    /**
     * @param elementType Sets elementType to the specified value.
     */
    public void setElementType( FieldMetadata elementType ) {
        this.elementType = elementType;
    }

    /**
     * @return componentType
     */
    public FieldMetadata getComponentType() {
        return componentType;
    }

    /**
     * @param componentType Sets componentType to the specified value.
     */
    public void setComponentType( FieldMetadata componentType ) {
        this.componentType = componentType;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ArrayTypeFieldMetadata [ " + getType() + " ]";
    }
}
