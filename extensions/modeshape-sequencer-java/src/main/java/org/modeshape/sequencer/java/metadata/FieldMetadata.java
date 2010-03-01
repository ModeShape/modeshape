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
package org.modeshape.sequencer.java.metadata;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * FieldMetadata is the base class of all fields.
 */
public class FieldMetadata {

    /** The type of the field */
    private String type;

    private String name;

    /** The variables */
    private List<Variable> variables = new ArrayList<Variable>();

    private List<ModifierMetadata> modifierMetadatas = new ArrayList<ModifierMetadata>();

    private List<AnnotationMetadata> annotations = new LinkedList<AnnotationMetadata>();
    
    
    public List<AnnotationMetadata> getAnnotations() {
        return annotations;
    }

    /**
     * @return variables
     */
    public List<Variable> getVariables() {
        return variables;
    }

    /**
     * @param variables Sets variables to the specified value.
     */
    public void setVariables( List<Variable> variables ) {
        this.variables = variables;
    }

    /**
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type Sets type to the specified value.
     */
    public void setType( String type ) {
        this.type = type;
    }

    /**
     * @return the name of the field
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the new name for the field
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return modifierMetadatas
     */
    public List<ModifierMetadata> getModifiers() {
        return modifierMetadatas;
    }

    /**
     * @param modifierMetadatas Sets modifierMetadatas to the specified value.
     */
    public void setModifiers( List<ModifierMetadata> modifierMetadatas ) {
        this.modifierMetadatas = modifierMetadatas;
    }

    /**
     * @param modifierName the name of the modifier to check for
     * @return true if the type has a modifier of that name, otherwise false
     */
    public boolean hasModifierNamed( String modifierName ) {
        for (ModifierMetadata modifier : modifierMetadatas) {
            if (modifierName.equals(modifier.getName())) {
                return true;
            }
        }

        return false;

    }

    /**
     * Find out if a field is primitive type or not.
     * 
     * @return true if field is a primitive type.
     */
    public boolean isPrimitiveType() {
        return false;
    }

    /**
     * Find out if a field is a simple type or not.
     * 
     * @return true if field is a simple type.
     */
    public boolean isSimpleType() {
        return false;
    }

    /**
     * Find out if a field is a array type or not.
     * 
     * @return true if field is a array type.
     */
    public boolean isArrayType() {
        return false;
    }

    /**
     * Find out if a field is a qualified type or not.
     * 
     * @return true if field is a qualified type.
     */
    public boolean isQualifiedType() {
        return false;
    }

    /**
     * Find out if a field is a parameterized type or not.
     * 
     * @return true if field is a parameterized type.
     */
    public boolean isParameterizedType() {
        return false;
    }

    /**
     * Find out if a field is a wild card type or not.
     * 
     * @return true if field is a wild card type.
     */
    public boolean isWildcardType() {
        return false;
    }
}
