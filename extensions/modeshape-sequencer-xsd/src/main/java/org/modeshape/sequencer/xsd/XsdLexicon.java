package org.modeshape.sequencer.xsd;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * A lexicon of names used within the XSD sequencer.
 */
@Immutable
public class XsdLexicon {
    public static class Namespace {
        public static final String URI = "http://www.w3.org/2001/XMLSchema";
        public static final String PREFIX = "xsd";
    }

    public static final Name SCHEMA_DOCUMENT = new BasicName(Namespace.URI, "schemaDocument");
    public static final Name COMPLEX_TYPE_DEFINITION = new BasicName(Namespace.URI, "complexTypeDefinition");
    public static final Name SIMPLE_TYPE_DEFINITION = new BasicName(Namespace.URI, "simpleTypeDefinition");
    public static final Name ATTRIBUTE_DECLARATION = new BasicName(Namespace.URI, "attributeDeclaration");
    public static final Name ELEMENT_DECLARATION = new BasicName(Namespace.URI, "elementDeclaration");
    public static final Name IMPORT = new BasicName(Namespace.URI, "import");
    public static final Name INCLUDE = new BasicName(Namespace.URI, "include");
    public static final Name REDEFINE = new BasicName(Namespace.URI, "redefine");
    public static final Name ATTRIBUTE_GROUP = new BasicName(Namespace.URI, "attributeGroup");
    public static final Name ANY_ATTRIBUTE = new BasicName(Namespace.URI, "anyAttribute");
    public static final Name ALL = new BasicName(Namespace.URI, "all");
    public static final Name CHOICE = new BasicName(Namespace.URI, "choice");
    public static final Name SEQUENCE = new BasicName(Namespace.URI, "sequence");
    public static final Name SIMPLE_CONTENT = new BasicName(Namespace.URI, "simpleContent");
    public static final Name COMPLEX_CONTENT = new BasicName(Namespace.URI, "complexContent");
    public static final Name COMPLEX_TYPE_CONTENT = new BasicName(Namespace.URI, "complexTypeContent");
    public static final Name ANNOTATION = new BasicName(Namespace.URI, "annotation");

    public static final Name IMPORTED_XSDS = new BasicName(Namespace.URI, "importedXsds");
    public static final Name INCLUDED_XSDS = new BasicName(Namespace.URI, "includedXsds");
    public static final Name REDEFINED_XSDS = new BasicName(Namespace.URI, "redefinedXsds");
    public static final Name NC_NAME = new BasicName(Namespace.URI, "ncName");
    public static final Name NAMESPACE = new BasicName(Namespace.URI, "namespace");
    public static final Name TYPE_NAME = new BasicName(Namespace.URI, "typeName");
    public static final Name TYPE_NAMESPACE = new BasicName(Namespace.URI, "typeNamespace");
    public static final Name TYPE_REFERENCE = new BasicName(Namespace.URI, "type");
    public static final Name BASE_TYPE_REFERENCE = new BasicName(Namespace.URI, "baseType");
    public static final Name BASE_TYPE_NAME = new BasicName(Namespace.URI, "baseTypeName");
    public static final Name BASE_TYPE_NAMESPACE = new BasicName(Namespace.URI, "baseTypeNamespace");
    public static final Name SCHEMA_LOCATION = new BasicName(Namespace.URI, "schemaLocation");
    public static final Name REF_NAMESPACE = new BasicName(Namespace.URI, "refNamespace");
    public static final Name REF_NAME = new BasicName(Namespace.URI, "refName");
    public static final Name REF = new BasicName(Namespace.URI, "ref");
    public static final Name METHOD = new BasicName(Namespace.URI, "method");

    public static final Name MIN_OCCURS = new BasicName(Namespace.URI, "minOccurs");
    public static final Name MAX_OCCURS = new BasicName(Namespace.URI, "maxOccurs");
    public static final Name MAX_OCCURS_UNBOUNDED = new BasicName(Namespace.URI, "maxOccursUnbounded");
    public static final Name MAX_LENGTH = new BasicName(Namespace.URI, "maxLength");
    public static final Name MIN_LENGTH = new BasicName(Namespace.URI, "minLength");
    public static final Name ENUMERATED_VALUES = new BasicName(Namespace.URI, "enumeratedValues");
    public static final Name WHITESPACE = new BasicName(Namespace.URI, "whitespace");
    public static final Name MAX_VALUE_EXCLUSIVE = new BasicName(Namespace.URI, "maxValueExclusive");
    public static final Name MIN_VALUE_EXCLUSIVE = new BasicName(Namespace.URI, "minValueExclusive");
    public static final Name MAX_VALUE_INCLUSIVE = new BasicName(Namespace.URI, "maxValueInclusive");
    public static final Name MIN_VALUE_INCLUSIVE = new BasicName(Namespace.URI, "minValueInclusive");
    public static final Name TOTAL_DIGITS = new BasicName(Namespace.URI, "totalDigits");
    public static final Name FRACTION_DIGITS = new BasicName(Namespace.URI, "fractionDigits");
    public static final Name PATTERN = new BasicName(Namespace.URI, "pattern");
    public static final Name FINAL = new BasicName(Namespace.URI, "final");
    public static final Name BLOCK = new BasicName(Namespace.URI, "block");
    public static final Name ABSTRACT = new BasicName(Namespace.URI, "abstract");
    public static final Name MIXED = new BasicName(Namespace.URI, "mixed");
    public static final Name NILLABLE = new BasicName(Namespace.URI, "nillable");
    public static final Name USE = new BasicName(Namespace.URI, "use");
    public static final Name PROCESS_CONTENTS = new BasicName(Namespace.URI, "processContents");
    public static final Name FORM = new BasicName(Namespace.URI, "form");

    public static final Name ID = new BasicName(Namespace.URI, "id");

    // Used as names for anonymous types ...
    public static final Name COMPLEX_TYPE = new BasicName(Namespace.URI, "complexType");
    public static final Name SIMPLE_TYPE = new BasicName(Namespace.URI, "simpleType");

}
