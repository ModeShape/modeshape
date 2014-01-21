/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
