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

    @Override
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

        buff.append(getTypeName()).append(' ');
        buff.append(getName()).append(';');

        return buff.toString();
    }
}
