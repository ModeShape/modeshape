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
package org.modeshape.sequencer.javafile;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.sequencer.Sequencer;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.*;
import org.modeshape.sequencer.classfile.metadata.Visibility;
import org.modeshape.sequencer.javafile.metadata.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A source file recorder that writes the Java metadata from the source file to the repository, using the same structure as the
 * default mode of the Java Class File sequencer.
 */
public class ClassSourceFileRecorder implements SourceFileRecorder {

    @Override
    public void record( Sequencer.Context context, Node outputNode, JavaMetadata javaMetadata ) throws RepositoryException {
        String packageName = javaMetadata.getPackageMetadata().getName();
        for (TypeMetadata typeMetadata : javaMetadata.getTypeMetadata()) {
            Node typeNode = getTypeNode(packageName, typeMetadata, outputNode);
            writeClassMetadata(context, typeNode, typeMetadata);
        }
    }

    private Node getTypeNode( String packageName, TypeMetadata typeMetadata, Node outputNode ) throws RepositoryException {
        String actualType = typeMetadata.getType().equals(TypeMetadata.Type.ENUM) ? ENUM : CLASS;
        String fullyQualifiedName = packageName + "." + typeMetadata.getName();
        for (String segment : fullyQualifiedName.split("\\.")) {
            outputNode = outputNode.addNode(segment);
        }
        outputNode.setPrimaryType(actualType);
        return outputNode;
    }

    private void writeClassMetadata( Sequencer.Context context, Node typeNode, TypeMetadata typeMetadata ) throws RepositoryException {
        setTypeMetaInformation(context, typeNode, typeMetadata);

        List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
        List<MethodMetadata> ctors = new ArrayList<MethodMetadata>();

        for (MethodMetadata method : typeMetadata.getMethods()) {
            if (method.getType() == MethodMetadata.Type.CONSTRUCTOR) {
                ctors.add(method);
            } else {
                methods.add(method);
            }
        }

        Node constructorsNode = typeNode.addNode(CONSTRUCTORS, CONSTRUCTORS);
        writeMethods(constructorsNode, ctors);

        Node methodsNode = typeNode.addNode(METHODS, METHODS);
        writeMethods(methodsNode, methods);

        Node fieldsNode = typeNode.addNode(FIELDS, FIELDS);
        writeFieldsNode(fieldsNode, typeMetadata.getFields());

        writeAnnotationsNode(typeNode, typeMetadata.getAnnotations());
    }

    private void setTypeMetaInformation( Sequencer.Context context, Node typeNode, TypeMetadata typeMetadata ) throws RepositoryException {
        /*
       - class:name (string) mandatory 
       - class:superTypeName (string) 
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
        typeNode.setProperty(NAME, typeMetadata.getName());
        typeNode.setProperty(SEQUENCED_DATE, context.getTimestamp());

        String superTypeName = typeMetadata.getSuperTypeName();
        if (StringUtil.isBlank(superTypeName)) {
            superTypeName = Object.class.getCanonicalName();
        }
        typeNode.setProperty(SUPER_CLASS_NAME, superTypeName);
        typeNode.setProperty(VISIBILITY, visibilityFor(typeMetadata).getDescription());
        typeNode.setProperty(ABSTRACT, typeMetadata.hasAbstractModifier());
        typeNode.setProperty(INTERFACE, typeMetadata.getType() == TypeMetadata.Type.INTERFACE);
        typeNode.setProperty(FINAL, typeMetadata.hasFinalModifier());
        typeNode.setProperty(STRICT_FP, typeMetadata.hasStrictFPModifier());
        typeNode.setProperty(INTERFACES, typeMetadata.getInterfaceNames().toArray(new String[0]));
        
        if (typeMetadata instanceof EnumMetadata) {
            typeNode.setProperty(ENUM_VALUES, ((EnumMetadata)typeMetadata).getValues().toArray(new String[0]));
        }
    }

    private Visibility visibilityFor( AbstractMetadata typeMetadata ) {
        if (typeMetadata.hasPublicVisibility()) {
            return Visibility.PUBLIC;
        }
        if (typeMetadata.hasProtectedVisibility()) {
            return Visibility.PROTECTED;
        }
        if (typeMetadata.hasPrivateVisibility()) {
            return Visibility.PRIVATE;
        }

        return Visibility.PACKAGE;
    }

    private void writeAnnotationsNode( Node rootNode, List<AnnotationMetadata> annotations ) throws RepositoryException {

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
        if (annotations.isEmpty()) {
            return;
        }

        Node annotationsContainer = rootNode.addNode(ANNOTATIONS, ANNOTATIONS);
        for (AnnotationMetadata annotationMetadata : annotations) {
            Node annotation = annotationsContainer.addNode(ANNOTATION, ANNOTATION);
            annotation.setProperty(NAME, annotationMetadata.getName());

            for (Map.Entry<String, String> entry : annotationMetadata.getMemberValues().entrySet()) {
                String key = entry.getKey();
                if (key == null) {
                    key = "default";
                }
                Node annotationMember = annotation.addNode(key, ANNOTATION_MEMBER);
                annotationMember.setProperty(NAME, key);
                annotationMember.setProperty(VALUE, entry.getValue());

            }
        }
    }

    private void writeFieldsNode( Node fields, List<FieldMetadata> fieldsMetadata ) throws RepositoryException {

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
        for (FieldMetadata fieldMetadata : fieldsMetadata) {
            Node field = fields.addNode(fieldMetadata.getName(), FIELD);
            field.setProperty(NAME, fieldMetadata.getName());
            field.setProperty(TYPE_CLASS_NAME, fieldMetadata.getType());
            field.setProperty(VISIBILITY, visibilityFor(fieldMetadata).getDescription());
            field.setProperty(STATIC, fieldMetadata.hasStaticModifier());
            field.setProperty(FINAL, fieldMetadata.hasFinalModifier());
            field.setProperty(TRANSIENT, fieldMetadata.hasTransientModifier());
            field.setProperty(VOLATILE, fieldMetadata.hasVolatileModifier());

            writeAnnotationsNode(field, fieldMetadata.getAnnotations());
        }
    }

    private void writeMethods( Node rootNode, List<MethodMetadata> methods ) throws RepositoryException {

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

        for (MethodMetadata methodMetadata : methods) {
            Node method = rootNode.addNode(methodMetadata.getId(), METHOD);

            method.setProperty(NAME, methodMetadata.getName());
            method.setProperty(RETURN_TYPE_CLASS_NAME, methodMetadata.getReturnTypeName());
            method.setProperty(VISIBILITY, visibilityFor(methodMetadata).getDescription());
            method.setProperty(STATIC, methodMetadata.hasStaticModifier());
            method.setProperty(FINAL, methodMetadata.hasFinalModifier());
            method.setProperty(ABSTRACT, methodMetadata.hasAbstractModifier());
            method.setProperty(STRICT_FP, methodMetadata.hasStrictFPModifier());
            method.setProperty(NATIVE, methodMetadata.hasNativeModifier());
            method.setProperty(SYNCHRONIZED, methodMetadata.hasSynchronizedModifier());
            method.setProperty(PARAMETERS, methodMetadata.getParameterTypes().toArray(new String[0]));

            writeAnnotationsNode(method, methodMetadata.getAnnotations());
        }

    }

}
