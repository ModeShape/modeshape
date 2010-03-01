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
import java.util.List;

/**
 * Exposes meta data of a top level type.
 */
public class TypeMetadata {

    public static final int PUBLIC_MODIFIER = 0;

    /** The name. */
    private String name;

    private String superClassName;

    /** All modifiers of a top level type */
    private List<ModifierMetadata> modifiers = new ArrayList<ModifierMetadata>();

    /** All annotations of a top level type */
    private List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

    /** All fields of a top level type */
    private List<FieldMetadata> fields = new ArrayList<FieldMetadata>();

    /** All methods of a top level type */
    private List<MethodMetadata> methods = new ArrayList<MethodMetadata>();

    /** All superinterfaces of a top level type */
    private final List<String> interfaceNames = new ArrayList<String>();

    /**
     * Get the name.
     * 
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name.
     * 
     * @param name Sets name to the specified value.
     */
    public void setName( String name ) {
        this.name = name;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public void setSuperClassName( String superClassName ) {
        this.superClassName = superClassName;
    }

    public List<String> getInterfaceNames() {
        return interfaceNames;
    }

    /**
     * @return annotations
     */
    public List<AnnotationMetadata> getAnnotations() {
        return annotations;
    }

    /**
     * @param annotations Sets annotations to the specified value.
     */
    public void setAnnotations( List<AnnotationMetadata> annotations ) {
        this.annotations = annotations;
    }

    /**
     * @return modifiers
     */
    public List<ModifierMetadata> getModifiers() {
        return modifiers;
    }

    /**
     * @param modifiers Sets modifiers to the specified value.
     */
    public void setModifiers( List<ModifierMetadata> modifiers ) {
        this.modifiers = modifiers;
    }

    /**
     * @param modifierName the name of the modifier to check for
     * @return true if the type has a modifier of that name, otherwise false
     */
    public boolean hasModifierNamed( String modifierName ) {
        for (ModifierMetadata modifier : modifiers) {
            if (modifierName.equals(modifier.getName())) {
                return true;
            }
        }

        return false;

    }

    /**
     * Gets a ordered lists of {@link FieldMetadata} from the unit.
     * 
     * @return all fields of this unit if there is one.
     */
    public List<FieldMetadata> getFields() {
        return this.fields;
    }

    /**
     * @param fields Sets fields to the specified value.
     */
    public void setFields( List<FieldMetadata> fields ) {
        this.fields = fields;
    }

    /**
     * Gets all {@link MethodMetadata} from the unit.
     * 
     * @return all methods from the units.
     */
    public List<MethodMetadata> getMethods() {
        return methods;
    }

    /**
     * @param methods Sets methods to the specified value.
     */
    public void setMethods( List<MethodMetadata> methods ) {
        this.methods = methods;
    }

}
