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
import java.util.List;

/**
 * Exposes meta data of a top level type.
 */
public class TypeMetadata extends AbstractMetadata {

    /** The name of the supertype */
    private String superTypeName;

    /** All fields of a top level type */
    private List<FieldMetadata> fields = new ArrayList<FieldMetadata>();

    /** All methods of a top level type */
    private List<MethodMetadata> methods = new ArrayList<MethodMetadata>();

    /** All superinterfaces of a top level type */
    private final List<String> interfaceNames = new ArrayList<String>();

    /** The type of metadata */
    private Type type;

    protected TypeMetadata( String name,
                            Type type ) {
        this.name = name;
        this.type = type;
    }

    public List<String> getInterfaceNames() {
        return interfaceNames;
    }

    /**
     * Gets a ordered lists of {@link org.modeshape.sequencer.javafile.metadata.FieldMetadata} from the unit.
     * 
     * @return all fields of this unit if there is one.
     */
    public List<FieldMetadata> getFields() {
        return this.fields;
    }

    /**
     * Gets all {@link org.modeshape.sequencer.javafile.metadata.MethodMetadata} from the unit.
     * 
     * @return all methods from the units.
     */
    public List<MethodMetadata> getMethods() {
        return methods;
    }

    public Type getType() {
        return type;
    }

    public String getSuperTypeName() {
        return superTypeName;
    }

    public void setSuperTypeName( String superTypeName ) {
        this.superTypeName = superTypeName;
    }

    public static TypeMetadata classType( String name ) {
        return new TypeMetadata(name, Type.CLASS);
    }

    public static EnumMetadata enumType( String name ) {
        return new EnumMetadata(name);
    }

    public static TypeMetadata interfaceType( String name ) {
        return new TypeMetadata(name, Type.INTERFACE);
    }

    public static enum Type {
        CLASS,
        ENUM,
        INTERFACE
    }
}
