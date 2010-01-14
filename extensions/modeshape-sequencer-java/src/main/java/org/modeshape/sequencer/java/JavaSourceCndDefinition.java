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
package org.modeshape.sequencer.java;

/**
 * JavaSourceCndDefinition defines each elements and sub elements, that must be found in the java source cnd document.
 */
public interface JavaSourceCndDefinition {

    public static final String SLASH = "/";

    public static final String JAVA_COMPILATION_UNIT_NODE = "java:compilationUnit";
    public static final String JAVA_COMPILATION_UNIT_PRIMARY_TYPE = "jcr:primaryType";

    // package declaration
    public static final String JAVA_PACKAGE_CHILD_NODE = "java:package";
    public static final String JAVA_PACKAGE_DECLARATION_CHILD_NODE = "java:packageDeclaration";
    public static final String JAVA_PACKAGE_NAME = "java:packageName";

    // Annnotation declaration
    public static final String JAVA_ANNOTATION_CHILD_NODE = "java:annotation";
    public static final String JAVA_ANNOTATION_DECLARATION_CHILD_NODE = "java:annotationDeclaration";
    public static final String JAVA_ANNOTATION_TYPE_CHILD_NODE = "java:annotationType";

    // Marker annotation
    public static final String JAVA_MARKER_ANNOTATION_CHILD_NODE = "java:markerAnnotation";
    public static final String JAVA_MARKER_ANNOTATION_NAME = "java:markerAnnotationName ";

    // Normal annotation
    public static final String JAVA_NORMAL_ANNOTATION_CHILD_NODE = "java:normalAnnotation";
    public static final String JAVA_NORMALANNOTATION_NAME = "java:normalAnnotationName";

    // Single element annotation
    public static final String JAVA_SINGLE_ELEMENT_ANNOTATION_CHILD_NODE = "java:singleElementAnnotation";
    public static final String JAVA_SINGLE_ANNOTATION_NAME = "java:singleElementAnnotationName";

    // Import declaration
    public static final String JAVA_IMPORT_CHILD_NODE = "java:import";
    public static final String JAVA_IMPORT_DECLARATION_CHILD_NODE = "java:importDeclaration";

    // Single import declaration
    public static final String JAVA_SINGLE_IMPORT_CHILD_NODE = "java:singleImport";
    public static final String JAVA_SINGLE_IMPORT_TYPE_DECLARATION_CHILD_NODE = "java:singleTypeImportDeclaration";
    public static final String JAVA_SINGLE_IMPORT_NAME = "java:singleImportName ";

    // OnDemand import declaration
    public static final String JAVA_ON_DEMAND_IMPORT_CHILD_NODE = "java:importOnDemand";
    public static final String JAVA_ON_DEMAND_IMPORT_TYPE_DECLARATION_CHILD_NODE = "java:typeImportOnDemandDeclaration";
    public static final String JAVA_ON_DEMAND_IMPORT_NAME = "java:onDemandImportName";

    // Class declaration
    public static final String JAVA_UNIT_TYPE_CHILD_NODE = "java:unitType";
    public static final String JAVA_CLASS_DECLARATION_CHILD_NODE = "java:classDeclaration";

    // Normal class declaration
    public static final String JAVA_NORMAL_CLASS_CHILD_NODE = "java:normalClass";
    public static final String JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE = "java:normalClassDeclaration";
    public static final String JAVA_NORMAL_CLASS_NAME = "java:normalClassName";

    // Modifier declaration
    public static final String JAVA_MODIFIER_CHILD_NODE = "java:modifier";
    public static final String JAVA_MODIFIER_DECLARATION_CHILD_NODE = "java:modifierDeclaration";
    public static final String JAVA_MODIFIER_NAME = "java:modifierName";

    // Variable declaration
    public static final String JAVA_VARIABLE = "java:variable";
    public static final String JAVA_VARIABLE_NAME = "java:variableName";

    // Primitive type
    public static final String JAVA_FIELD_CHILD_NODE = "java:field";
    public static final String JAVA_FIELD_TYPE_CHILD_NODE = "java:fieldType";
    public static final String JAVA_TYPE_CHILD_NODE = "java:type";
    public static final String JAVA_PRIMITIVE_TYPE_CHILD_NODE = "java:primitiveType";
    public static final String JAVA_PRIMITIVE_TYPE_NAME = "java:primitiveTypeName";
    public static final String JAVA_PRIMITIVE_TYPE_VARIABLE = "java:primitiveVariable";

    // Method declaration
    public static final String JAVA_METHOD_CHILD_NODE = "java:method";
    public static final String JAVA_METHOD_DECLARATION_CHILD_NODE = "java:methodDeclaration";
    public static final String JAVA_METHOD_NAME = "java:methodName";

    // Constructor
    public static final String JAVA_CONSTRUCTOR_CHILD_NODE = "java:constructor";
    public static final String JAVA_CONSTRUCTOR_DECLARATION_CHILD_NODE = "java:constructorDeclaration";
    public static final String JAVA_CONSTRUCTOR_NAME = "java:constructorName";

    // Parameter
    public static final String JAVA_PARAMETER = "java:parameter";
    public static final String JAVA_FORMAL_PARAMETER = "java:formalParameter";
    public static final String JAVA_PARAMETER_NAME = "java:parameterName";

    public static final String JAVA_RETURN_TYPE = "java:resultType";

    // Simple type
    public static final String JAVA_SIMPLE_TYPE_CHILD_NODE = "java:simpleType";
    public static final String JAVA_SIMPLE_TYPE_DESCRIPTION = "java:simpleTypeDescription";
    public static final String JAVA_SIMPLE_TYPE_NAME = "java:simpleTypeName";
    public static final String JAVA_SIMPLE_TYPE_VARIABLE = "java:simpleTypeVariable";
    public static final String JAVA_SIMPLE_TYPE_MODIFIER_CHILD_NODE = "java:simpleTypeModifier";

    // Parameterized type
    public static final String JAVA_PARAMETERIZED_TYPE_CHILD_NODE = "java:parameterizedType";
    public static final String JAVA_PARAMETERIZED_TYPE_DESCRIPTION = "java:parameterizedTypeDescription";
    public static final String JAVA_PARAMETERIZED_TYPE_MODIFIER_CHILD_NODE = "java:parameterizedTypeModifier";
    public static final String JAVA_PARAMETERIZED_TYPE_NAME = "java:parameterizedTypeName";
    public static final String JAVA_PARAMETERIZED_TYPE_VARIABLE = "java:parameterizedTypeVariable";

    // Array type
    public static final String JAVA_ARRAY_TYPE_CHILD_NODE = "java:arrayType";
    public static final String JAVA_ARRAY_TYPE_DESCRIPTION = "java:arrayTypeDescription";
    public static final String JAVA_ARRAY_TYPE_MODIFIER_CHILD_NODE = "java:arrayTypeModifier";
    public static final String JAVA_ARRAY_TYPE_NAME = "java:arrayTypeName";
    public static final String JAVA_ARRAY_TYPE_VARIABLE = "java:arrayTypeVariable";

}
