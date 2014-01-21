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
 * Represent the {@link org.modeshape.sequencer.javafile.metadata.MethodMetadata}
 */
public class MethodMetadata extends AbstractMetadata {

    private Type type;

    private FieldMetadata returnType;

    private List<FieldMetadata> parameters = new ArrayList<FieldMetadata>();

    private MethodMetadata( String name,
                            Type type ) {
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

            buff.append(shortNameFor(parameter.getType()).replace("[]", " array"));
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
        CONSTRUCTOR,
        METHOD_TYPE_MEMBER
    }
}
