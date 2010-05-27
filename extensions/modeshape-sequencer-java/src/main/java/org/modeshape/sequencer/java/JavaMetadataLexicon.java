package org.modeshape.sequencer.java;

import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * A lexicon of names used within the Java source file sequencer.
 */
@Immutable
public class JavaMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/java/1.0";
        public static final String PREFIX = "java";
    }

    public static final Name COMPILATION_UNIT_NODE = new BasicName(Namespace.URI, "compilationUnit");
    public static final Name PACKAGE_CHILD_NODE = new BasicName(Namespace.URI, "package");
    public static final Name PACKAGE_DECLARATION_CHILD_NODE = new BasicName(Namespace.URI, "packageDeclaration");
    public static final Name PACKAGE_NAME = new BasicName(Namespace.URI, "packageName");
    public static final Name ANNOTATION_CHILD_NODE = new BasicName(Namespace.URI, "annotation");
    public static final Name ANNOTATION_DECLARATION_CHILD_NODE = new BasicName(Namespace.URI, "annotationDeclaration");
    public static final Name ANNOTATION_TYPE_CHILD_NODE = new BasicName(Namespace.URI, "annotationType");
    public static final Name MARKER_ANNOTATION_CHILD_NODE = new BasicName(Namespace.URI, "markerAnnotation");
    public static final Name MARKER_ANNOTATION_NAME = new BasicName(Namespace.URI, "markerAnnotationName");
    public static final Name NORMAL_ANNOTATION_CHILD_NODE = new BasicName(Namespace.URI, "normalAnnotation");
    public static final Name NORMALANNOTATION_NAME = new BasicName(Namespace.URI, "normalAnnotationName");
    public static final Name SINGLE_ELEMENT_ANNOTATION_CHILD_NODE = new BasicName(Namespace.URI, "singleElementAnnotation");
    public static final Name SINGLE_ANNOTATION_NAME = new BasicName(Namespace.URI, "singleElementAnnotationName");
    public static final Name IMPORT_CHILD_NODE = new BasicName(Namespace.URI, "import");
    public static final Name IMPORT_DECLARATION_CHILD_NODE = new BasicName(Namespace.URI, "importDeclaration");
    public static final Name SINGLE_IMPORT_CHILD_NODE = new BasicName(Namespace.URI, "singleImport");
    public static final Name SINGLE_IMPORT_TYPE_DECLARATION_CHILD_NODE = new BasicName(Namespace.URI,
                                                                                       "singleTypeImportDeclaration");
    public static final Name SINGLE_IMPORT_NAME = new BasicName(Namespace.URI, "singleImportName");
    public static final Name ON_DEMAND_IMPORT_CHILD_NODE = new BasicName(Namespace.URI, "importOnDemand");
    public static final Name ON_DEMAND_IMPORT_TYPE_DECLARATION_CHILD_NODE = new BasicName(Namespace.URI,
                                                                                          "importOnDemandDeclaration");
    public static final Name ON_DEMAND_IMPORT_NAME = new BasicName(Namespace.URI, "onDemandImportName");
    public static final Name UNIT_TYPE_CHILD_NODE = new BasicName(Namespace.URI, "unitType");
    public static final Name CLASS_DECLARATION_CHILD_NODE = new BasicName(Namespace.URI, "classDeclaration");
    public static final Name NORMAL_CLASS_CHILD_NODE = new BasicName(Namespace.URI, "normalClass");
    public static final Name NORMAL_CLASS_DECLARATION_CHILD_NODE = new BasicName(Namespace.URI, "normalClassDeclaration");
    public static final Name NORMAL_CLASS_NAME = new BasicName(Namespace.URI, "normalClassName");
    public static final Name MODIFIER_CHILD_NODE = new BasicName(Namespace.URI, "modifier");
    public static final Name MODIFIER_DECLARATION_CHILD_NODE = new BasicName(Namespace.URI, "modifierDeclaration");
    public static final Name MODIFIER_NAME = new BasicName(Namespace.URI, "modifierName");
    public static final Name VARIABLE = new BasicName(Namespace.URI, "variable");
    public static final Name VARIABLE_NAME = new BasicName(Namespace.URI, "variableName");
    public static final Name FIELD_CHILD_NODE = new BasicName(Namespace.URI, "field");
    public static final Name FIELD_TYPE_CHILD_NODE = new BasicName(Namespace.URI, "fieldType");
    public static final Name TYPE_CHILD_NODE = new BasicName(Namespace.URI, "type");
    public static final Name PRIMITIVE_TYPE_CHILD_NODE = new BasicName(Namespace.URI, "primitiveType");
    public static final Name PRIMITIVE_TYPE_NAME = new BasicName(Namespace.URI, "primitiveTypeName");
    public static final Name PRIMITIVE_TYPE_VARIABLE = new BasicName(Namespace.URI, "primitiveVariable");
    public static final Name METHOD_CHILD_NODE = new BasicName(Namespace.URI, "method");
    public static final Name METHOD_DECLARATION_CHILD_NODE = new BasicName(Namespace.URI, "methodDeclaration");
    public static final Name METHOD_NAME = new BasicName(Namespace.URI, "methodName");
    public static final Name CONSTRUCTOR_CHILD_NODE = new BasicName(Namespace.URI, "constructor");
    public static final Name CONSTRUCTOR_DECLARATION_CHILD_NODE = new BasicName(Namespace.URI, "constructorDeclaration");
    public static final Name CONSTRUCTOR_NAME = new BasicName(Namespace.URI, "constructorName");
    public static final Name PARAMETER = new BasicName(Namespace.URI, "parameter");
    public static final Name FORMAL_PARAMETER = new BasicName(Namespace.URI, "formalParameter");
    public static final Name PARAMETER_NAME = new BasicName(Namespace.URI, "parameterName");
    public static final Name RETURN_TYPE = new BasicName(Namespace.URI, "resultType");
    public static final Name SIMPLE_TYPE_CHILD_NODE = new BasicName(Namespace.URI, "simpleType");
    public static final Name SIMPLE_TYPE_DESCRIPTION = new BasicName(Namespace.URI, "simpleTypeDescription");
    public static final Name SIMPLE_TYPE_NAME = new BasicName(Namespace.URI, "simpleTypeName");
    public static final Name SIMPLE_TYPE_VARIABLE = new BasicName(Namespace.URI, "simpleTypeVariable");
    public static final Name SIMPLE_TYPE_MODIFIER_CHILD_NODE = new BasicName(Namespace.URI, "simpleTypeModifier");
    public static final Name PARAMETERIZED_TYPE_CHILD_NODE = new BasicName(Namespace.URI, "parameterizedType");
    public static final Name PARAMETERIZED_TYPE_DESCRIPTION = new BasicName(Namespace.URI, "parameterizedTypeDescription");
    public static final Name PARAMETERIZED_TYPE_MODIFIER_CHILD_NODE = new BasicName(Namespace.URI, "parameterizedTypeModifier");
    public static final Name PARAMETERIZED_TYPE_NAME = new BasicName(Namespace.URI, "parameterizedTypeName");
    public static final Name PARAMETERIZED_TYPE_VARIABLE = new BasicName(Namespace.URI, "parameterizedTypeVariable");
    public static final Name ARRAY_TYPE_CHILD_NODE = new BasicName(Namespace.URI, "arrayType");
    public static final Name ARRAY_TYPE_DESCRIPTION = new BasicName(Namespace.URI, "arrayTypeDescription");
    public static final Name ARRAY_TYPE_MODIFIER_CHILD_NODE = new BasicName(Namespace.URI, "arrayTypeModifier");
    public static final Name ARRAY_TYPE_NAME = new BasicName(Namespace.URI, "arrayTypeName");
    public static final Name ARRAY_TYPE_VARIABLE = new BasicName(Namespace.URI, "arrayTypeVariable");

    // Original constants - for future debugging purposes
    // public static final String JAVA_COMPILATION_UNIT_NODE = "java:compilationUnit";
    // public static final String JAVA_PACKAGE_CHILD_NODE = "java:package";
    // public static final String JAVA_PACKAGE_DECLARATION_CHILD_NODE = "java:packageDeclaration";
    // public static final String JAVA_PACKAGE_NAME = "java:packageName";
    // public static final String JAVA_ANNOTATION_CHILD_NODE = "java:annotation";
    // public static final String JAVA_ANNOTATION_DECLARATION_CHILD_NODE = "java:annotationDeclaration";
    // public static final String JAVA_ANNOTATION_TYPE_CHILD_NODE = "java:annotationType";
    // public static final String JAVA_MARKER_ANNOTATION_CHILD_NODE = "java:markerAnnotation";
    // public static final String JAVA_MARKER_ANNOTATION_NAME = "java:markerAnnotationName ";
    // public static final String JAVA_NORMAL_ANNOTATION_CHILD_NODE = "java:normalAnnotation";
    // public static final String JAVA_NORMALANNOTATION_NAME = "java:normalAnnotationName";
    // public static final String JAVA_SINGLE_ELEMENT_ANNOTATION_CHILD_NODE = "java:singleElementAnnotation";
    // public static final String JAVA_SINGLE_ANNOTATION_NAME = "java:singleElementAnnotationName";
    // public static final String JAVA_IMPORT_CHILD_NODE = "java:import";
    // public static final String JAVA_IMPORT_DECLARATION_CHILD_NODE = "java:importDeclaration";
    // public static final String JAVA_SINGLE_IMPORT_CHILD_NODE = "java:singleImport";
    // public static final String JAVA_SINGLE_IMPORT_TYPE_DECLARATION_CHILD_NODE = "java:singleTypeImportDeclaration";
    // public static final String JAVA_SINGLE_IMPORT_NAME = "java:singleImportName ";
    // public static final String JAVA_ON_DEMAND_IMPORT_CHILD_NODE = "java:importOnDemand";
    // public static final String JAVA_ON_DEMAND_IMPORT_TYPE_DECLARATION_CHILD_NODE = "java:typeImportOnDemandDeclaration";
    // public static final String JAVA_ON_DEMAND_IMPORT_NAME = "java:onDemandImportName";
    // public static final String JAVA_UNIT_TYPE_CHILD_NODE = "java:unitType";
    // public static final String JAVA_CLASS_DECLARATION_CHILD_NODE = "java:classDeclaration";
    // public static final String JAVA_NORMAL_CLASS_CHILD_NODE = "java:normalClass";
    // public static final String JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE = "java:normalClassDeclaration";
    // public static final String JAVA_NORMAL_CLASS_NAME = "java:normalClassName";
    // public static final String JAVA_MODIFIER_CHILD_NODE = "java:modifier";
    // public static final String JAVA_MODIFIER_DECLARATION_CHILD_NODE = "java:modifierDeclaration";
    // public static final String JAVA_MODIFIER_NAME = "java:modifierName";
    // public static final String JAVA_VARIABLE = "java:variable";
    // public static final String JAVA_VARIABLE_NAME = "java:variableName";
    // public static final String JAVA_FIELD_CHILD_NODE = "java:field";
    // public static final String JAVA_FIELD_TYPE_CHILD_NODE = "java:fieldType";
    // public static final String JAVA_TYPE_CHILD_NODE = "java:type";
    // public static final String JAVA_PRIMITIVE_TYPE_CHILD_NODE = "java:primitiveType";
    // public static final String JAVA_PRIMITIVE_TYPE_NAME = "java:primitiveTypeName";
    // public static final String JAVA_PRIMITIVE_TYPE_VARIABLE = "java:primitiveVariable";
    // public static final String JAVA_METHOD_CHILD_NODE = "java:method";
    // public static final String JAVA_METHOD_DECLARATION_CHILD_NODE = "java:methodDeclaration";
    // public static final String JAVA_METHOD_NAME = "java:methodName";
    // public static final String JAVA_CONSTRUCTOR_CHILD_NODE = "java:constructor";
    // public static final String JAVA_CONSTRUCTOR_DECLARATION_CHILD_NODE = "java:constructorDeclaration";
    // public static final String JAVA_CONSTRUCTOR_NAME = "java:constructorName";
    // public static final String JAVA_PARAMETER = "java:parameter";
    // public static final String JAVA_FORMAL_PARAMETER = "java:formalParameter";
    // public static final String JAVA_PARAMETER_NAME = "java:parameterName";
    // public static final String JAVA_RETURN_TYPE = "java:resultType";
    // public static final String JAVA_SIMPLE_TYPE_CHILD_NODE = "java:simpleType";
    // public static final String JAVA_SIMPLE_TYPE_DESCRIPTION = "java:simpleTypeDescription";
    // public static final String JAVA_SIMPLE_TYPE_NAME = "java:simpleTypeName";
    // public static final String JAVA_SIMPLE_TYPE_VARIABLE = "java:simpleTypeVariable";
    // public static final String JAVA_SIMPLE_TYPE_MODIFIER_CHILD_NODE = "java:simpleTypeModifier";
    // public static final String JAVA_PARAMETERIZED_TYPE_CHILD_NODE = "java:parameterizedType";
    // public static final String JAVA_PARAMETERIZED_TYPE_DESCRIPTION = "java:parameterizedTypeDescription";
    // public static final String JAVA_PARAMETERIZED_TYPE_MODIFIER_CHILD_NODE = "java:parameterizedTypeModifier";
    // public static final String JAVA_PARAMETERIZED_TYPE_NAME = "java:parameterizedTypeName";
    // public static final String JAVA_PARAMETERIZED_TYPE_VARIABLE = "java:parameterizedTypeVariable";
    // public static final String JAVA_ARRAY_TYPE_CHILD_NODE = "java:arrayType";
    // public static final String JAVA_ARRAY_TYPE_DESCRIPTION = "java:arrayTypeDescription";
    // public static final String JAVA_ARRAY_TYPE_MODIFIER_CHILD_NODE = "java:arrayTypeModifier";
    // public static final String JAVA_ARRAY_TYPE_NAME = "java:arrayTypeName";
    // public static final String JAVA_ARRAY_TYPE_VARIABLE = "java:arrayTypeVariable";

}
