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

    protected TypeMetadata( String name, Type type ) {
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

    public static TypeMetadata classType(String name) {
        return new TypeMetadata(name, Type.CLASS);
    }
    
    public static EnumMetadata enumType(String name) {
        return new EnumMetadata(name);
    }

    public static TypeMetadata interfaceType(String name) {
        return new TypeMetadata(name, Type.INTERFACE);
    }

    public static enum Type {
        CLASS, ENUM, INTERFACE
    }
}
