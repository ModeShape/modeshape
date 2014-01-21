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
 * FieldMetadata is the base class of all fields.
 */
public class FieldMetadata extends AbstractMetadata {

    /** The type of the metadata */
    private Type metadataType;

    /** The type of the field */
    private String type;

    /** The variables */
    private List<Variable> variables = new ArrayList<Variable>();

    private FieldMetadata( String type,
                           Type metadataType ) {
        this.type = type;
        this.metadataType = metadataType;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public String getType() {
        return type;
    }

    public Type getMetadataType() {
        return metadataType;
    }

    public static FieldMetadata primitiveType( String type ) {
        return new FieldMetadata(type, Type.PRIMITIVE);
    }

    public static FieldMetadata simpleType( String type ) {
        return new FieldMetadata(type, Type.SIMPLE);
    }

    public static FieldMetadata arrayType( String type ) {
        return new FieldMetadata(type, Type.ARRAY);
    }

    public static FieldMetadata qualifiedType( String type ) {
        return new FieldMetadata(type, Type.QUALIFIED);
    }

    public static FieldMetadata parametrizedType( String type ) {
        return new FieldMetadata(type, Type.PARAMETRIZED);
    }

    public static FieldMetadata wildcardType( String type ) {
        return new FieldMetadata(type, Type.WILDCARD);
    }

    public static enum Type {
        PRIMITIVE,
        SIMPLE,
        ARRAY,
        QUALIFIED,
        PARAMETRIZED,
        WILDCARD
    }
}
