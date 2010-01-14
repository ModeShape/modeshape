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
package org.modeshape.sequencer.classfile.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;

public class MethodMetadata implements Comparable<MethodMetadata> {

    private final MethodInfo method;
    private final String name;
    private final List<AnnotationMetadata> annotations;
    private final List<String> parameters;

    MethodMetadata( ClassFile clazz,
                    MethodInfo method ) {
        this.method = method;
        this.name = method.isConstructor() ? clazz.getName() : method.getName();
        this.annotations = annotationsFor(method);
        this.parameters = parametersFor(method);
    }

    private List<String> parametersFor( MethodInfo method ) {
        String descriptor = method.getDescriptor();
        int lastParenPos = descriptor.lastIndexOf(')');
        assert lastParenPos >= 0;
        String parameterString = descriptor.substring(1, lastParenPos);

        if (parameterString.length() == 0) {
            return Collections.emptyList();
        }

        List<String> parameters = new ArrayList<String>();
        Descriptor.Iterator iter = new Descriptor.Iterator(parameterString);
        
        assert iter.hasNext();
        int startPos = iter.next();

        while (iter.hasNext()) {
            int endPos = iter.next();
            parameters.add(Descriptor.toClassName(parameterString.substring(startPos, endPos)));
            startPos = endPos;
        }
        parameters.add(Descriptor.toClassName(parameterString.substring(startPos)));

        return parameters;
    }

    private String returnTypeFor( MethodInfo method ) {
        String descriptor = method.getDescriptor();
        int lastParenPos = descriptor.lastIndexOf(')');
        assert lastParenPos >= 0;
        return Descriptor.toClassName(descriptor.substring(lastParenPos + 1));
    }

    private List<AnnotationMetadata> annotationsFor( MethodInfo method ) {
        List<AnnotationMetadata> annotations = new LinkedList<AnnotationMetadata>();

        for (Object ob : method.getAttributes()) {
            AttributeInfo att = (AttributeInfo)ob;

            if (att instanceof AnnotationsAttribute) {
                for (Annotation ann : ((AnnotationsAttribute)att).getAnnotations()) {
                    annotations.add(new AnnotationMetadata(ann));
                }
            }
        }

        return annotations;
    }

    private String shortNameFor( String type ) {
        int lastDotPos = type.lastIndexOf('.');
        if (lastDotPos < 0) return type;
        return type.substring(lastDotPos + 1);
    }

    public String getName() {
        return name;
    }

    public Visibility getVisibility() {
        return Visibility.fromAccessFlags(method.getAccessFlags());
    }

    public boolean isConstructor() {
        return method.isConstructor();
    }

    public String getReturnType() {
        return returnTypeFor(method);
    }

    public List<AnnotationMetadata> getAnnotations() {
        return annotations;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public boolean isStatic() {
        return AccessFlag.STATIC == (AccessFlag.STATIC & method.getAccessFlags());
    }

    public boolean isFinal() {
        return AccessFlag.FINAL == (AccessFlag.FINAL & method.getAccessFlags());
    }

    public boolean isAbstract() {
        return AccessFlag.ABSTRACT == (AccessFlag.ABSTRACT & method.getAccessFlags());
    }

    public boolean isNative() {
        return AccessFlag.NATIVE == (AccessFlag.NATIVE & method.getAccessFlags());
    }

    public boolean isStrictFp() {
        return AccessFlag.STRICT == (AccessFlag.STRICT & method.getAccessFlags());
    }

    public boolean isSynchronized() {
        return AccessFlag.SYNCHRONIZED == (AccessFlag.SYNCHRONIZED & method.getAccessFlags());
    }

    public int compareTo( MethodMetadata o ) {
        if (this.isStatic() && !o.isStatic()) {
            return -1;
        }
        if (!this.isStatic() && o.isStatic()) {
            return 1;
        }

        if (!this.name.equals(o.name)) {
            return this.name.compareTo(o.name);
        }

        if (this.parameters.size() != o.parameters.size()) {
            return ((Integer)this.parameters.size()).compareTo(o.parameters.size());
        }

        for (int i = 0; i < parameters.size(); i++) {
            String p1 = this.parameters.get(i);
            String p2 = o.parameters.get(i);

            if (!p1.equals(p2)) {
                return p1.compareTo(p2);
            }
        }

        return 0;
    }

    public String getId() {
        StringBuilder buff = new StringBuilder();
        buff.append(method.getName()).append('(');
        
        boolean first = false;
        for (String parameter : parameters) {
            if (first) {
                first = false;
            } else {
                buff.append(", ");
            }
            buff.append(shortNameFor(parameter).replace("[]", " array"));
        }

        buff.append(')');
        
        return buff.toString();
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();

        if (!annotations.isEmpty()) {
            for (AnnotationMetadata annotation : annotations) {
                buff.append(annotation).append("\n\t");
            }
        }

        buff.append(getVisibility());
        if (getVisibility() != Visibility.PACKAGE) buff.append(' ');

        if (isFinal()) buff.append("final ");
        if (isStatic()) buff.append("static ");

        buff.append(shortNameFor(getReturnType())).append(' ');
        buff.append(name).append('(');

        boolean first = true;

        for (String parameter : parameters) {
            if (first) {
                first = false;
            } else {
                buff.append(", ");
            }
            buff.append(shortNameFor(parameter));
        }

        buff.append(' ');
        
        buff.append(");");

        return buff.toString();
    }

}
