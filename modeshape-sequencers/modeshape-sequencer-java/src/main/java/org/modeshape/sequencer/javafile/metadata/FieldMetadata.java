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


    private FieldMetadata( String type, Type metadataType ) {
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

    public static FieldMetadata primitiveType(String type) {
        return new FieldMetadata(type, Type.PRIMITIVE);
    }

    public static FieldMetadata simpleType(String type) {
        return new FieldMetadata(type, Type.SIMPLE);
    }

    public static FieldMetadata arrayType(String type) {
        return new FieldMetadata(type, Type.ARRAY);
    }

    public static FieldMetadata qualifiedType(String type) {
        return new FieldMetadata(type, Type.QUALIFIED);
    }

    public static FieldMetadata parametrizedType(String type) {
        return new FieldMetadata(type, Type.PARAMETRIZED);
    }

    public static FieldMetadata wildcardType(String type) {
        return new FieldMetadata(type, Type.WILDCARD);
    }
  
    public static enum Type {
        PRIMITIVE, SIMPLE, ARRAY, QUALIFIED, PARAMETRIZED, WILDCARD
    }
}
