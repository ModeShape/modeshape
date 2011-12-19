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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.sequencer.Sequencer;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.*;
import org.modeshape.sequencer.classfile.metadata.*;
import java.util.List;
import java.util.Map;

public class DefaultClassFileRecorder implements ClassFileRecorder {

    @Override
    public void recordClass( Sequencer.Context context, Node outputNode, ClassMetadata classMetadata ) throws RepositoryException {

        Node classNode = getClassNode(classMetadata, outputNode);

        writeClassMetaInformation(context, classMetadata, classNode);

        Node constructorsNode = classNode.addNode(CONSTRUCTORS, CONSTRUCTORS);
        writeMethods(constructorsNode, classMetadata.getConstructors());

        Node methodsNode = classNode.addNode(METHODS, METHODS);
        writeMethods(methodsNode, classMetadata.getMethods());

        Node fieldsNode = classNode.addNode(FIELDS, FIELDS);
        writeFieldsNode(fieldsNode, classMetadata.getFields());

    }

    private void writeClassMetaInformation( Sequencer.Context context, ClassMetadata classMetadata, Node classNode ) throws RepositoryException {
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

    private Node getClassNode( ClassMetadata classMetadata, Node outputNode ) throws RepositoryException {
        String actualType = classMetadata.isEnumeration() ? ENUM : CLASS;
        //if the output node is an existing node, create a series of nt:unstructured nodes as the package path
        if (!outputNode.isNew()) {
            for (String packageName : classMetadata.getClassName().split("\\.")) {
                outputNode = outputNode.addNode(packageName);
            }
        }
        outputNode.setPrimaryType(actualType);
        return outputNode;
    }

    private void writeFieldsNode( Node fieldsNode, List<FieldMetadata> fields ) throws RepositoryException {

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
            methodNode.setProperty(PARAMETERS, method.getParameters().toArray(new String[0]));

            writeAnnotationsNode(methodNode, method.getAnnotations());
        }
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
}
