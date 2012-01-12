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
 * Represent the {@link org.modeshape.sequencer.javafile.metadata.MethodMetadata}
 */
public class MethodMetadata extends AbstractMetadata {

    private Type type;

    private FieldMetadata returnType;

    private List<FieldMetadata> parameters = new ArrayList<FieldMetadata>();

    private MethodMetadata( String name, Type type ) {
        this.name = name;
        this.type = type;
    }

    /**
     * @return parameters
     */
    public List<FieldMetadata> getParameters() {
        return parameters;
    }

    /**
     * @return parameters
     */
    public List<String> getParameterTypes() {
        List<String> params = new ArrayList<String>(parameters.size());
        for (FieldMetadata param : parameters) {
            params.add(param.getType());
        }
        return params;
    }

    /**
     * @return returnType
     */
    public FieldMetadata getReturnType() {
        return returnType;
    }

    public String getReturnTypeName() {
        return returnType == null ? Void.TYPE.getCanonicalName() : returnType.getType();
    }

    /**
     * @param returnType Sets returnType to the specified value.
     */
    public void setReturnType( FieldMetadata returnType ) {
        this.returnType = returnType;
    }

    public String getId() {
        StringBuilder buff = new StringBuilder();
        buff.append(getName()).append('(');

        boolean first = true;
        for (FieldMetadata parameter : parameters) {
            if (first) {
                first = false;
            } else {
                buff.append(", ");
            }

            buff.append(shortNameFor(parameter.getName()).replace("[]", " array"));
        }

        buff.append(')');

        return buff.toString();
    }

    private String shortNameFor( String type ) {
        assert type != null;

        int lastDotPos = type.lastIndexOf('.');
        if (lastDotPos < 0) {
            return type;
        }
        return type.substring(lastDotPos + 1);
    }

    public Type getType() {
        return type;
    }

    public static MethodMetadata constructorType( String name ) {
        return new MethodMetadata(name, Type.CONSTRUCTOR);
    }

    public static MethodMetadata methodMemberType( String name ) {
        return new MethodMetadata(name, Type.METHOD_TYPE_MEMBER);
    }

    public static enum Type {
        CONSTRUCTOR, METHOD_TYPE_MEMBER
    }
}
