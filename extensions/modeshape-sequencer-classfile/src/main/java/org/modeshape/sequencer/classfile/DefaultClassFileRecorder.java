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
package org.modeshape.sequencer.classfile;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.classfile.metadata.AnnotationMetadata;
import org.modeshape.sequencer.classfile.metadata.ClassMetadata;
import org.modeshape.sequencer.classfile.metadata.EnumMetadata;
import org.modeshape.sequencer.classfile.metadata.FieldMetadata;
import org.modeshape.sequencer.classfile.metadata.MethodMetadata;

public class DefaultClassFileRecorder implements ClassFileRecorder {

    public void recordClass( StreamSequencerContext context,
                             SequencerOutput output,
                             ClassMetadata classMetadata ) {

        PathFactory pathFactory = pathFactoryFor(context);
        DateTimeFactory dateFactory = dateFactoryFor(context);
        Path classPath = pathFor(pathFactory, classMetadata);

        writeClassNode(output, pathFactory, dateFactory, classPath, classMetadata);
    }

    private DateTimeFactory dateFactoryFor( StreamSequencerContext context ) {
        return context.getValueFactories().getDateFactory();
    }

    private PathFactory pathFactoryFor( StreamSequencerContext context ) {
        return context.getValueFactories().getPathFactory();
    }

    private Path pathFor( PathFactory pathFactory,
                          ClassMetadata cmd ) {
        List<Segment> segments = new LinkedList<Segment>();

        for (String segment : cmd.getClassName().split("\\.")) {
            segments.add(pathFactory.createSegment(segment));
        }

        return pathFactory.createRelativePath(segments);
    }

    private void writeClassNode( SequencerOutput output,
                                 PathFactory pathFactory,
                                 DateTimeFactory dateFactory,
                                 Path classPath,
                                 ClassMetadata cmd ) {

        /*
        - class:name (string) mandatory 
        - class:superClassName (string) 
        - class:visibility (string) mandatory < 'public', 'protected', 'package', 'private'
        - class:abstract (boolean) mandatory
        - class:interface (boolean) mandatory
        - class:final (boolean) mandatory
        - class:strictFp (boolean) mandatory
        - class:interfaces (string) multiple
        + class:annotations (class:annotations) = class:annotations
        + class:constructors (class:constructors) = class:constructors
        + class:methods (class:methods) = class:methods
        + class:fields (class:fields) = class:fields
         */

        output.setProperty(classPath, ClassFileSequencerLexicon.NAME, cmd.getClassName());
        output.setProperty(classPath, ClassFileSequencerLexicon.SEQUENCED_DATE, dateFactory.create());
        String superClassName = cmd.getSuperclassName();
        if (superClassName == null || superClassName.length() == 0) {
            superClassName = Object.class.getCanonicalName();
        }
        output.setProperty(classPath, ClassFileSequencerLexicon.SUPER_CLASS_NAME, superClassName);
        output.setProperty(classPath, ClassFileSequencerLexicon.VISIBILITY, cmd.getVisibility().getDescription());
        output.setProperty(classPath, ClassFileSequencerLexicon.ABSTRACT, cmd.isAbstract());
        output.setProperty(classPath, ClassFileSequencerLexicon.INTERFACE, cmd.isInterface());
        output.setProperty(classPath, ClassFileSequencerLexicon.FINAL, cmd.isFinal());
        output.setProperty(classPath, ClassFileSequencerLexicon.STRICT_FP, cmd.isStrictFp());
        output.setProperty(classPath, ClassFileSequencerLexicon.INTERFACES, (Object[])cmd.getInterfaces());

        Path constructorsPath = pathFactory.create(classPath, ClassFileSequencerLexicon.CONSTRUCTORS);
        output.setProperty(constructorsPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.CONSTRUCTORS);
        writeMethods(output, pathFactory, constructorsPath, cmd.getConstructors());

        Path methodsPath = pathFactory.create(classPath, ClassFileSequencerLexicon.METHODS);
        output.setProperty(methodsPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.METHODS);
        writeMethods(output, pathFactory, methodsPath, cmd.getMethods());

        writeFieldsNode(output, pathFactory, classPath, cmd.getFields());
        writeAnnotationsNode(output, pathFactory, classPath, cmd.getAnnotations());

        if (cmd instanceof EnumMetadata) {
            output.setProperty(classPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.ENUM);

            output.setProperty(classPath, ClassFileSequencerLexicon.ENUM_VALUES, ((EnumMetadata)cmd).getValues().toArray());
        } else {
            output.setProperty(classPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.CLASS);
        }
    }

    private void writeFieldsNode( SequencerOutput output,
                                  PathFactory pathFactory,
                                  Path classPath,
                                  List<FieldMetadata> fields ) {

        /*
            [class:field]
            - class:name (string) mandatory 
            - class:typeClassName (string) mandatory 
            - class:visibility (string) mandatory < 'public', 'protected', 'package', 'private'
            - class:static (boolean) mandatory
            - class:final (boolean) mandatory
            - class:transient (boolean) mandatory
            - class:volatile (boolean) mandatory
            + class:annotations (class:annotations) = class:annotations
            
            [class:fields]
            + * (class:field) = class:field
         */

        Path fieldsPath = pathFactory.create(classPath, ClassFileSequencerLexicon.FIELDS);
        output.setProperty(fieldsPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.FIELDS);

        for (FieldMetadata field : fields) {
            Path fieldPath = pathFactory.create(fieldsPath, field.getName());

            output.setProperty(fieldPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.FIELD);
            output.setProperty(fieldPath, ClassFileSequencerLexicon.NAME, field.getName());
            output.setProperty(fieldPath, ClassFileSequencerLexicon.TYPE_CLASS_NAME, field.getTypeName());
            output.setProperty(fieldPath, ClassFileSequencerLexicon.VISIBILITY, field.getVisibility().getDescription());
            output.setProperty(classPath, ClassFileSequencerLexicon.STATIC, field.isStatic());
            output.setProperty(classPath, ClassFileSequencerLexicon.FINAL, field.isFinal());
            output.setProperty(classPath, ClassFileSequencerLexicon.TRANSIENT, field.isTransient());
            output.setProperty(classPath, ClassFileSequencerLexicon.VOLATILE, field.isVolatile());

            writeAnnotationsNode(output, pathFactory, fieldPath, field.getAnnotations());

        }
    }

    private void writeMethods( SequencerOutput output,
                               PathFactory pathFactory,
                               Path methodsPath,
                               List<MethodMetadata> methods ) {

        /*
            [class:method]
            - class:name (string) mandatory 
            - class:returnTypeClassName (string) mandatory 
            - class:visibility (string) mandatory < 'public', 'protected', 'package', 'private'
            - class:static (boolean) mandatory
            - class:final (boolean) mandatory
            - class:abstract (boolean) mandatory
            - class:strictFp (boolean) mandatory
            - class:native (boolean) mandatory
            - class:synchronized (boolean) mandatory
            - class:parameters (string) multiple
            + class:annotations (class:annotations) = class:annotations
         */

        for (MethodMetadata method : methods) {
            Path methodPath = pathFactory.create(methodsPath, method.getId());

            output.setProperty(methodPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.METHOD);
            output.setProperty(methodPath, ClassFileSequencerLexicon.NAME, method.getName());
            output.setProperty(methodPath, ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME, method.getReturnType());
            output.setProperty(methodPath, ClassFileSequencerLexicon.VISIBILITY, method.getVisibility().getDescription());
            output.setProperty(methodPath, ClassFileSequencerLexicon.STATIC, method.isStatic());
            output.setProperty(methodPath, ClassFileSequencerLexicon.FINAL, method.isFinal());
            output.setProperty(methodPath, ClassFileSequencerLexicon.ABSTRACT, method.isAbstract());
            output.setProperty(methodPath, ClassFileSequencerLexicon.STRICT_FP, method.isStrictFp());
            output.setProperty(methodPath, ClassFileSequencerLexicon.NATIVE, method.isNative());
            output.setProperty(methodPath, ClassFileSequencerLexicon.SYNCHRONIZED, method.isSynchronized());
            output.setProperty(methodPath, ClassFileSequencerLexicon.PARAMETERS, method.getParameters().toArray());

            writeAnnotationsNode(output, pathFactory, methodPath, method.getAnnotations());

        }

    }

    private void writeAnnotationsNode( SequencerOutput output,
                                       PathFactory pathFactory,
                                       Path parentPath,
                                       List<AnnotationMetadata> annotations ) {

        /*
        [class:annotationMember]
        - class:name (string) mandatory
        - class:value (string) 
        
        [class:annotation]
        - class:name (string) mandatory
        + * (class:annotationMember) = class:annotationMember
        
        [class:annotations]
        + * (class:annotation) = class:annotation
         */

        Path annotationsPath = pathFactory.create(parentPath, ClassFileSequencerLexicon.ANNOTATIONS);
        output.setProperty(annotationsPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.ANNOTATIONS);

        for (AnnotationMetadata annotation : annotations) {
            Path annotationPath = pathFactory.create(parentPath, annotation.getAnnotationClassName());
            output.setProperty(annotationPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.ANNOTATION);

            for (Map.Entry<String, String> entry : annotation.getMemberValues().entrySet()) {
                Path annotationMemberPath = pathFactory.create(annotationPath, entry.getKey());
                output.setProperty(annotationMemberPath, JcrLexicon.PRIMARY_TYPE, ClassFileSequencerLexicon.ANNOTATION_MEMBER);
                output.setProperty(annotationMemberPath, ClassFileSequencerLexicon.NAME, entry.getKey());
                output.setProperty(annotationMemberPath, ClassFileSequencerLexicon.VALUE, entry.getValue());

            }

        }
    }
}
