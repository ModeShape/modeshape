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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.javafile.metadata;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Base class for the other type metadata used when retrieving information from the JDT compiler.
 *
 * @author Horia Chiorean
 */
public abstract class AbstractMetadata {

    public static final String STATIC = "static";
    public static final String FINAL = "final";
    public static final String ABSTRACT = "abstract";
    public static final String STRICT_FP = "strictfp";
    public static final String NATIVE = "native";
    public static final String SYNCRHONIZED = "synchronized";
    public static final String TRANSIENT = "transient";
    public static final String VOLATILE = "volatile";

    public static final String PUBLIC = "public";
    public static final String PROTECTED = "protected";
    public static final String PRIVATE = "private";

    protected String name;

    private List<ModifierMetadata> modifiers = new ArrayList<ModifierMetadata>();
    private List<AnnotationMetadata> annotations = new LinkedList<AnnotationMetadata>();


    public List<AnnotationMetadata> getAnnotations() {
        return annotations;
    }

    public List<ModifierMetadata> getModifiers() {
        return modifiers;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public boolean hasStaticModifier() {
        return hasModifierNamed(STATIC);
    }
    
    public boolean hasFinalModifier() {
        return hasModifierNamed(FINAL);
    }

    public boolean hasAbstractModifier() {
        return hasModifierNamed(ABSTRACT);
    }

    public boolean hasNativeModifier() {
        return hasModifierNamed(NATIVE);
    }

    public boolean hasSynchronizedModifier() {
        return hasModifierNamed(SYNCRHONIZED);
    }

    public boolean hasStrictFPModifier() {
        return hasModifierNamed(STRICT_FP);
    }

    public boolean hasTransientModifier() {
        return hasModifierNamed(TRANSIENT);
    }

    public boolean hasVolatileModifier() {
        return hasModifierNamed(VOLATILE);
    }

    public boolean hasPublicVisibility() {
        return hasModifierNamed(PUBLIC);
    }

    public boolean hasProtectedVisibility() {
        return hasModifierNamed(PROTECTED);
    }

    public boolean hasPrivateVisibility() {
        return hasModifierNamed(PRIVATE);
    }

    /**
     * Checks if a modifier with the given name is found among this method's identifiers.
     *
     * @param modifierName the name of the modifier to check for
     * @return true if the type has a modifier of that name, otherwise false
     */
    private boolean hasModifierNamed( String modifierName ) {
        for (ModifierMetadata modifier : modifiers) {
            if (modifierName.equalsIgnoreCase(modifier.getName())) {
                return true;
            }
        }

        return false;

    }
}
