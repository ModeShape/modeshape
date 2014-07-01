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
package org.modeshape.sequencer.javafile;

import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.ABSTRACT;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.ANNOTATION;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.ANNOTATIONS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.ANNOTATION_MEMBER;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.CLASS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.CONSTRUCTORS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.ENUM;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.ENUM_VALUES;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.FIELD;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.FIELDS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.FINAL;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.IMPORTS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.INTERFACE;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.INTERFACES;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.METHOD;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.METHODS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.METHOD_PARAMETERS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.NAME;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.NATIVE;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.PACKAGE;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.PARAMETER;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.PARAMETERS;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.SEQUENCED_DATE;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.STATIC;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.STRICT_FP;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.SUPER_CLASS_NAME;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.SYNCHRONIZED;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.TRANSIENT;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.TYPE_CLASS_NAME;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.VALUE;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.VISIBILITY;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.VOLATILE;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.jcr.api.sequencer.Sequencer.Context;
import org.modeshape.sequencer.classfile.metadata.Visibility;
import org.modeshape.sequencer.javafile.metadata.AbstractMetadata;
import org.modeshape.sequencer.javafile.metadata.AnnotationMetadata;
import org.modeshape.sequencer.javafile.metadata.EnumMetadata;
import org.modeshape.sequencer.javafile.metadata.FieldMetadata;
import org.modeshape.sequencer.javafile.metadata.ImportMetadata;
import org.modeshape.sequencer.javafile.metadata.JavaMetadata;
import org.modeshape.sequencer.javafile.metadata.MethodMetadata;
import org.modeshape.sequencer.javafile.metadata.TypeMetadata;

/**
 * A source file recorder that writes the Java metadata from the source file to the repository, using the same structure as the
 * default mode of the Java Class File sequencer.
 */
public class ClassSourceFileRecorder implements SourceFileRecorder {

    /**
     * {@inheritDoc}
     *
     * @see org.modeshape.sequencer.javafile.SourceFileRecorder#record(org.modeshape.jcr.api.sequencer.Sequencer.Context, java.io.InputStream, long, java.lang.String, javax.jcr.Node)
     */
    @Override
    public void record( final Context context,
                        final InputStream inputStream,
                        final long length,
                        final String encoding,
                        final Node outputNode ) throws Exception {
            JavaMetadata javaMetadata = JavaMetadata.instance(inputStream, length, encoding);
            record(context, outputNode, javaMetadata);
    }

    private void record( Sequencer.Context context,
                        Node outputNode,
                        JavaMetadata javaMetadata ) throws RepositoryException {
        String packageName = javaMetadata.getPackageMetadata().getName();
        for (TypeMetadata typeMetadata : javaMetadata.getTypeMetadata()) {
            Node typeNode = getTypeNode(packageName, typeMetadata, outputNode);
            writeClassMetadata(context, typeNode, typeMetadata);
            writeImports(typeNode, javaMetadata.getImports());
        }
    }

    private Node getTypeNode( String packageName,
                              TypeMetadata typeMetadata,
                              Node outputNode ) throws RepositoryException {
        final String[] packagePath = packageName.split("\\.");

        if (packageName.length() > 0) {
            for (final String pkg : packagePath) {
                outputNode = outputNode.addNode(pkg);
                outputNode.addMixin(PACKAGE);
            }
        }

        final Node classNode = outputNode.addNode(typeMetadata.getName());
        final String actualType = typeMetadata.getType().equals(TypeMetadata.Type.ENUM) ? ENUM : CLASS;
        classNode.setPrimaryType(actualType);

        return classNode;
    }

    private void writeClassMetadata( Sequencer.Context context,
                                     Node typeNode,
                                     TypeMetadata typeMetadata ) throws RepositoryException {
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

    private void setTypeMetaInformation( Sequencer.Context context,
                                         Node typeNode,
                                         TypeMetadata typeMetadata ) throws RepositoryException {
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

    private void writeAnnotationsNode( Node rootNode,
                                       List<AnnotationMetadata> annotations ) throws RepositoryException {

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

    private void writeFieldsNode( Node fields,
                                  List<FieldMetadata> fieldsMetadata ) throws RepositoryException {

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

    private void writeMethods( Node rootNode,
                               List<MethodMetadata> methods ) throws RepositoryException {

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
            - class:parameters (string) multiple // NO LONGER USED!
            + class:annotations (class:annotations) = class:annotations
            + class:methodParameters (class:parameters) = class:parameters
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
            writeParameters(method, methodMetadata.getParameters());
            writeAnnotationsNode(method, methodMetadata.getAnnotations());
        }

    }

    private void writeParameters( Node method,
                                  List<FieldMetadata> fieldsMetadata ) throws RepositoryException {
        // Always create the container node ...
        Node parametersContainer = method.addNode(METHOD_PARAMETERS, PARAMETERS);
        if (!fieldsMetadata.isEmpty()) {
            /*
                [class:parameters]
                + * (class:parameter) = class:parameter

                [class:parameter]
                - class:name (string) mandatory
                - class:typeClassName (string) mandatory
                - class:final (boolean) mandatory
                + class:annotations (class:annotations) = class:annotations
             */
            for (FieldMetadata fieldMetadata : fieldsMetadata) {
                Node field = parametersContainer.addNode(fieldMetadata.getName(), PARAMETER);
                field.setProperty(NAME, fieldMetadata.getName());
                field.setProperty(TYPE_CLASS_NAME, fieldMetadata.getType());
                field.setProperty(FINAL, fieldMetadata.hasFinalModifier());
                writeAnnotationsNode(field, fieldMetadata.getAnnotations());
            }
        }
    }

    private void writeImports( final Node typeNode,
                               final List<ImportMetadata> imports ) throws RepositoryException {
        if ((imports != null) && !imports.isEmpty()) {
            final String[] values = new String[imports.size()];
            int i = 0;

            for (final ImportMetadata importMetadata : imports) {
                values[i++] = importMetadata.getName();
            }

            typeNode.setProperty(IMPORTS, values);
        }
    }

}
