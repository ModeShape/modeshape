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
import javassist.bytecode.Descriptor;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.annotation.Annotation;

public class FieldMetadata implements Comparable<FieldMetadata> {

    private final FieldInfo field;
    private final List<AnnotationMetadata> annotations;

    FieldMetadata( FieldInfo field ) {
        this.field = field;
        this.annotations = annotationsFor(field);
    }

    private List<AnnotationMetadata> annotationsFor( FieldInfo field ) {
        List<AnnotationMetadata> annotations = new LinkedList<AnnotationMetadata>();

        for (Object ob : field.getAttributes()) {
            AttributeInfo att = (AttributeInfo)ob;

            if (att instanceof AnnotationsAttribute) {
                for (Annotation ann : ((AnnotationsAttribute)att).getAnnotations()) {
                    annotations.add(new AnnotationMetadata(ann));
                }
            }
        }

        return Collections.unmodifiableList(annotations);
    }

    public String getName() {
        return field.getName();
    }

    public boolean isStatic() {
        return AccessFlag.STATIC == (AccessFlag.STATIC & field.getAccessFlags());
    }

    public boolean isFinal() {
        return AccessFlag.FINAL == (AccessFlag.FINAL & field.getAccessFlags());
    }

    public boolean isTransient() {
        return AccessFlag.TRANSIENT == (AccessFlag.TRANSIENT & field.getAccessFlags());
    }

    public boolean isVolatile() {
        return AccessFlag.VOLATILE == (AccessFlag.VOLATILE & field.getAccessFlags());
    }

    public Visibility getVisibility() {
        return Visibility.fromAccessFlags(field.getAccessFlags());
    }

    public String getTypeName() {
        return Descriptor.toClassName(field.getDescriptor());
    }

    public List<AnnotationMetadata> getAnnotations() {
        return annotations;
    }

    public int compareTo( FieldMetadata o ) {
        if (this.isStatic() && !o.isStatic()) {
            return 1;
        }
        if (!this.isStatic() && o.isStatic()) {
            return -1;
        }

        return this.getName().compareTo(o.getName());
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
        if (isTransient()) buff.append("transient ");

        buff.append(Descriptor.toClassName(getTypeName())).append(' ');
        buff.append(getName()).append(';');

        return buff.toString();
    }
}
