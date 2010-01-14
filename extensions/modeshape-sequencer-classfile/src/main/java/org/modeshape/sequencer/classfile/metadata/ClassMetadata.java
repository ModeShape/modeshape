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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import net.jcip.annotations.Immutable;

@Immutable
public class ClassMetadata {

    private final ClassFile clazz;

    private final List<AnnotationMetadata> annotations;
    private final List<FieldMetadata> fields;
    private final List<MethodMetadata> methods;
    private final List<MethodMetadata> constructors;

    ClassMetadata( ClassFile clazz ) {
        this.clazz = clazz;
        this.annotations = annotationsFor(clazz);
        this.fields = fieldsFor(clazz);
        this.methods = methodsFor(clazz);
        this.constructors = constructorsFor(clazz);
    }

    private List<AnnotationMetadata> annotationsFor( ClassFile clazz ) {
        List<AnnotationMetadata> annotations = new LinkedList<AnnotationMetadata>();

        for (Object ob : clazz.getAttributes()) {
            AttributeInfo att = (AttributeInfo)ob;

            if (att instanceof AnnotationsAttribute) {
                for (Annotation ann : ((AnnotationsAttribute)att).getAnnotations()) {
                    annotations.add(new AnnotationMetadata(ann));
                }
            }
        }

        return Collections.unmodifiableList(annotations);
    }

    private List<FieldMetadata> fieldsFor( ClassFile clazz ) {
        List<FieldMetadata> fields = new LinkedList<FieldMetadata>();

        for (Object field : clazz.getFields()) {
            fields.add(new FieldMetadata((FieldInfo)field));
        }

        Collections.sort(fields);

        return Collections.unmodifiableList(fields);
    }

    private List<MethodMetadata> methodsFor( ClassFile clazz ) {
        List<MethodMetadata> methods = new LinkedList<MethodMetadata>();

        for (Object ob : clazz.getMethods()) {
            MethodInfo method = (MethodInfo)ob;

            if (!method.isStaticInitializer() && !method.isConstructor()) {
                methods.add(new MethodMetadata(clazz, method));
            }
        }

        Collections.sort(methods);

        return Collections.unmodifiableList(methods);
    }

    private List<MethodMetadata> constructorsFor( ClassFile clazz ) {
        List<MethodMetadata> ctors = new LinkedList<MethodMetadata>();

        for (Object ob : clazz.getMethods()) {
            MethodInfo method = (MethodInfo)ob;
            if (!method.isStaticInitializer() && method.isConstructor()) {
                ctors.add(new MethodMetadata(clazz, method));
            }
        }

        Collections.sort(ctors);

        return Collections.unmodifiableList(ctors);
    }

    public boolean isEnumeration() {
        return false;
    }

    public String getClassName() {
        return clazz.getName();
    }

    public String getSuperclassName() {
        return clazz.getSuperclass();
    }

    public String[] getInterfaces() {
        return clazz.getInterfaces();
    }

    public boolean isAbstract() {
        return clazz.isAbstract();
    }

    public boolean isInterface() {
        return clazz.isInterface();
    }

    public boolean isStrictFp() {
        return AccessFlag.STRICT == (AccessFlag.STRICT & clazz.getAccessFlags());
    }

    public boolean isFinal() {
        return AccessFlag.FINAL == (AccessFlag.FINAL & clazz.getAccessFlags());
    }

    public Visibility getVisibility() {
        return Visibility.fromAccessFlags(clazz.getAccessFlags());
    }

    public List<AnnotationMetadata> getAnnotations() {
        return annotations;
    }

    public List<FieldMetadata> getFields() {
        return fields;
    }

    public List<MethodMetadata> getMethods() {
        return methods;
    }

    public List<MethodMetadata> getConstructors() {
        return constructors;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();

        for (AnnotationMetadata annotation : annotations) {
            buff.append(annotation).append('\n');
        }

        buff.append(getVisibility());
        if (getVisibility() != Visibility.PACKAGE) {
            buff.append(' ');
        }

        if (isAbstract()) {
            if (isInterface()) {
                buff.append("interface ");
            } else {
                buff.append("abstract class ");

            }
        } else {
            buff.append("class ");
        }

        if (getSuperclassName() != null && !Object.class.getName().equals(getSuperclassName())) {
            buff.append(" extends ").append(getSuperclassName()).append(" ");
        }

        if (getInterfaces().length > 0) {
            boolean first = true;
            buff.append(" implements ");

            for (String interfaceName : getInterfaces()) {
                if (first) {
                    first = false;
                } else {
                    buff.append(", ");
                }
                buff.append(interfaceName);
            }

            buff.append(' ');
        }

        buff.append(getClassName()).append(" {\n");

        for (FieldMetadata field : fields) {
            buff.append('\t').append(field).append('\n');
        }

        if (!methods.isEmpty()) {
            buff.append('\n');
        }

        for (MethodMetadata method : methods) {
            buff.append('\t').append(method).append('\n');
        }

        buff.append("}");

        return buff.toString();
    }
}
