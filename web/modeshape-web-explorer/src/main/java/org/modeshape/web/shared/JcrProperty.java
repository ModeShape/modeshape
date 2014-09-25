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
package org.modeshape.web.shared;

import java.io.Serializable;

/**
 * Node property object descriptor.
 * 
 * @author kulikov
 */
public class JcrProperty implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String type;
    private String value;
    private boolean isProtected;
    private boolean isMultiValue;

    public JcrProperty() {
    }

    /**
     * Creates new property object.
     * 
     * @param name the name of the property.
     * @param type text description for the type
     * @param value text view of the value.
     */
    public JcrProperty( String name,
                        String type,
                        String value ) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    /**
     * Gets name of the property.
     * 
     * @return name of the property.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets property type.
     * 
     * @return type of the property.
     */
    public String getType() {
        return type;
    }

    /**
     * Gets value of the property.
     * 
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     * Modifies name of the property.
     * 
     * @param name the new name for the property.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Modifies type of the property.
     * 
     * @param type the new type of the property.
     */
    public void setType( String type ) {
        this.type = type;
    }

    /**
     * Modifies value of the property.
     * 
     * @param value
     */
    public void setValue( String value ) {
        this.value = value;
    }

    public boolean isMultiValue() {
        return this.isMultiValue;
    }

    /**
     * Marks property as multiple or single value.
     * 
     * @param isMultiValue true if property has multiple value
     */
    public void setMultiValue( boolean isMultiValue ) {
        this.isMultiValue = isMultiValue;
    }

    public boolean isProtected() {
        return this.isProtected;
    }

    /**
     * Marks property either protected or not.
     * 
     * @param isProtected true if property is protected false otherwise.
     */
    public void setProtected( boolean isProtected ) {
        this.isProtected = isProtected;
    }
}
