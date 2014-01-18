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
package org.modeshape.sequencer.classfile;

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
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.classfile.metadata.AnnotationMetadata;
import org.modeshape.sequencer.classfile.metadata.ClassMetadata;
import org.modeshape.sequencer.classfile.metadata.EnumMetadata;
import org.modeshape.sequencer.classfile.metadata.FieldMetadata;
import org.modeshape.sequencer.classfile.metadata.MethodMetadata;
import org.modeshape.sequencer.javafile.metadata.ImportMetadata;

public class DefaultClassFileRecorder implements ClassFileRecorder {

    @Override
    public void recordClass( Sequencer.Context context,
                             Node outputNode,
                             ClassMetadata classMetadata ) throws RepositoryException {

        Node classNode = getClassNode(classMetadata, outputNode);

        writeClassMetaInformation(context, classMetadata, classNode);

        Node constructorsNode = classNode.addNode(CONSTRUCTORS, CONSTRUCTORS);
        writeMethods(constructorsNode, classMetadata.getConstructors());

        Node methodsNode = classNode.addNode(METHODS, METHODS);
        writeMethods(methodsNode, classMetadata.getMethods());

        Node fieldsNode = classNode.addNode(FIELDS, FIELDS);
        writeFieldsNode(fieldsNode, classMetadata.getFields());

        writeImports(classNode, classMetadata.getImports());
    }

    private void writeClassMetaInformation( Sequencer.Context context,
                                            ClassMetadata classMetadata,
                                            Node classNode ) throws RepositoryException {
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

        classNode.setProperty(NAME, classMetadata.getClassName());
        classNode.setProperty(SEQUENCED_DATE, context.getTimestamp());
        String superClassName = classMetadata.getSuperclassName();
        if (StringUtil.isBlank(superClassName)) {
            superClassName = Object.class.getCanonicalName();
        }
        classNode.setProperty(SUPER_CLASS_NAME, superClassName);
        classNode.setProperty(VISIBILITY, classMetadata.getVisibility().getDescription());
        classNode.setProperty(ABSTRACT, classMetadata.isAbstract());
        classNode.setProperty(INTERFACE, classMetadata.isInterface());
        classNode.setProperty(FINAL, classMetadata.isFinal());
        classNode.setProperty(STRICT_FP, classMetadata.isStrictFp());
        classNode.setProperty(INTERFACES, classMetadata.getInterfaces());

        if (ENUM.equalsIgnoreCase(classNode.getProperty(JcrConstants.JCR_PRIMARY_TYPE).getString())) {
            classNode.setProperty(ENUM_VALUES, ((EnumMetadata)classMetadata).getValues().toArray(new String[0]));
        }
    }

    private Node getClassNode( ClassMetadata classMetadata,
                               Node outputNode ) throws RepositoryException {
        final String[] packagePath = classMetadata.getClassName().split("\\.");
        int i = 0;

        // create a series of nt:unstructured nodes as the package path
        if (packagePath.length > 1) {
            for (final int numPkgs = (packagePath.length - 1); i < numPkgs; ++i) {
                outputNode = outputNode.addNode(packagePath[i]);
                outputNode.addMixin(PACKAGE);
            }
        }

        final Node classNode = outputNode.addNode(packagePath[i]);
        final String actualType = (classMetadata.isEnumeration() ? ENUM : CLASS);
        classNode.setPrimaryType(actualType);

        return classNode;
    }

    private void writeFieldsNode( Node fieldsNode,
                                  List<FieldMetadata> fields ) throws RepositoryException {

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
         */
        for (FieldMetadata field : fields) {
            Node fieldNode = fieldsNode.addNode(field.getName(), FIELD);

            fieldNode.setProperty(NAME, field.getName());
            fieldNode.setProperty(TYPE_CLASS_NAME, field.getTypeName());
            fieldNode.setProperty(VISIBILITY, field.getVisibility().getDescription());
            fieldNode.setProperty(STATIC, field.isStatic());
            fieldNode.setProperty(FINAL, field.isFinal());
            fieldNode.setProperty(TRANSIENT, field.isTransient());
            fieldNode.setProperty(VOLATILE, field.isVolatile());

            writeAnnotationsNode(fieldNode, field.getAnnotations());
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
            - class:parameters (string) multiple
            + class:methodParameters (class:parameters) = class:parameters
            + class:annotations (class:annotations) = class:annotations

            [class:parameters]
            + * (class:parameter) = class:parameter

            [class:parameter]
            - class:name (string) mandatory 
            - class:typeClassName (string) mandatory 
            - class:final (boolean) mandatory
            + class:annotations (class:annotations) = class:annotations
         */

        for (MethodMetadata method : methods) {
            Node methodNode = rootNode.addNode(method.getId(), METHOD);
            methodNode.setProperty(NAME, method.getName());
            methodNode.setProperty(RETURN_TYPE_CLASS_NAME, method.getReturnType());
            methodNode.setProperty(VISIBILITY, method.getVisibility().getDescription());
            methodNode.setProperty(STATIC, method.isStatic());
            methodNode.setProperty(FINAL, method.isFinal());
            methodNode.setProperty(ABSTRACT, method.isAbstract());
            methodNode.setProperty(STRICT_FP, method.isStrictFp());
            methodNode.setProperty(NATIVE, method.isNative());
            methodNode.setProperty(SYNCHRONIZED, method.isSynchronized());
            Node parametersNode = methodNode.addNode(METHOD_PARAMETERS, PARAMETERS);
            int num = 0;
            for (String paramType : method.getParameters()) {
                String paramName = "p" + (++num); // not included in bytecode
                Node field = parametersNode.addNode(paramName, PARAMETER);
                field.setProperty(NAME, paramName);
                field.setProperty(TYPE_CLASS_NAME, paramType);
                field.setProperty(FINAL, false);
            }
            writeAnnotationsNode(methodNode, method.getAnnotations());
        }
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

        Node annotationsNode = rootNode.addNode(ANNOTATIONS, ANNOTATIONS);

        for (AnnotationMetadata annotation : annotations) {
            Node annotationNode = annotationsNode.addNode(ANNOTATION, ANNOTATION);
            annotationNode.setProperty(NAME, annotation.getAnnotationClassName());

            for (Map.Entry<String, String> entry : annotation.getMemberValues().entrySet()) {
                Node annotationMember = annotationNode.addNode(entry.getKey(), ANNOTATION_MEMBER);
                annotationMember.setProperty(NAME, entry.getKey());
                annotationMember.setProperty(VALUE, entry.getValue());
            }
        }
    }

    private void writeImports( final Node classNode,
                               final List<ImportMetadata> imports ) throws RepositoryException {
        assert (classNode != null);
        assert (imports != null);

        if (!imports.isEmpty()) {
            final String[] values = new String[imports.size()];
            int i = 0;

            for (final ImportMetadata imported : imports) {
                values[i++] = imported.getName();
            }

            classNode.setProperty(IMPORTS, values);
        }
    }

}
