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
package org.modeshape.sequencer.xsd;

import static org.modeshape.sequencer.xsd.XsdLexicon.Namespace.PREFIX;
import org.modeshape.common.annotation.Immutable;


/**
 * A lexicon of names used within the XSD sequencer.
 */
@Immutable
public class XsdLexicon {
    private XsdLexicon() {
    }

    public static class Namespace {
        public static final String URI = "http://www.w3.org/2001/XMLSchema";
        public static final String PREFIX = "xs";
    }

    public static final String SCHEMA_DOCUMENT = PREFIX + ":schemaDocument";
    public static final String COMPLEX_TYPE_DEFINITION = PREFIX + ":complexTypeDefinition";
    public static final String SIMPLE_TYPE_DEFINITION = PREFIX + ":simpleTypeDefinition";
    public static final String ATTRIBUTE_DECLARATION = PREFIX + ":attributeDeclaration";
    public static final String ELEMENT_DECLARATION = PREFIX + ":elementDeclaration";
    public static final String IMPORT = PREFIX + ":import";
    public static final String INCLUDE = PREFIX + ":include";
    public static final String REDEFINE = PREFIX + ":redefine";
    public static final String ATTRIBUTE_GROUP = PREFIX + ":attributeGroup";
    public static final String ANY_ATTRIBUTE = PREFIX + ":anyAttribute";
    public static final String ALL = PREFIX + ":all";
    public static final String CHOICE = PREFIX + ":choice";
    public static final String SEQUENCE = PREFIX + ":sequence";
    public static final String SIMPLE_CONTENT = PREFIX + ":simpleContent";
    public static final String COMPLEX_CONTENT = PREFIX + ":complexContent";
    public static final String COMPLEX_TYPE_CONTENT = PREFIX + ":complexTypeContent";
    public static final String ANNOTATION = PREFIX + ":annotation";

    public static final String IMPORTED_XSDS = PREFIX + ":importedXsds";
    public static final String INCLUDED_XSDS = PREFIX + ":includedXsds";
    public static final String REDEFINED_XSDS = PREFIX + ":redefinedXsds";
    public static final String NC_NAME = PREFIX + ":ncName";
    public static final String NAMESPACE = PREFIX + ":namespace";
    public static final String TYPE_NAME = PREFIX + ":typeName";
    public static final String TYPE_NAMESPACE = PREFIX + ":typeNamespace";
    public static final String TYPE_REFERENCE = PREFIX + ":type";
    public static final String BASE_TYPE_REFERENCE = PREFIX + ":baseType";
    public static final String BASE_TYPE_NAME = PREFIX + ":baseTypeName";
    public static final String BASE_TYPE_NAMESPACE = PREFIX + ":baseTypeNamespace";
    public static final String SCHEMA_LOCATION = PREFIX + ":schemaLocation";
    public static final String REF_NAMESPACE = PREFIX + ":refNamespace";
    public static final String REF_NAME = PREFIX + ":refName";
    public static final String REF = PREFIX + ":ref";
    public static final String METHOD = PREFIX + ":method";

    public static final String MIN_OCCURS = PREFIX + ":minOccurs";
    public static final String MAX_OCCURS = PREFIX + ":maxOccurs";
    public static final String MAX_OCCURS_UNBOUNDED = PREFIX + ":maxOccursUnbounded";
    public static final String MAX_LENGTH = PREFIX + ":maxLength";
    public static final String MIN_LENGTH = PREFIX + ":minLength";
    public static final String ENUMERATED_VALUES = PREFIX + ":enumeratedValues";
    public static final String WHITESPACE = PREFIX + ":whitespace";
    public static final String MAX_VALUE_EXCLUSIVE = PREFIX + ":maxValueExclusive";
    public static final String MIN_VALUE_EXCLUSIVE = PREFIX + ":minValueExclusive";
    public static final String MAX_VALUE_INCLUSIVE = PREFIX + ":maxValueInclusive";
    public static final String MIN_VALUE_INCLUSIVE = PREFIX + ":minValueInclusive";
    public static final String TOTAL_DIGITS = PREFIX + ":totalDigits";
    public static final String FRACTION_DIGITS = PREFIX + ":fractionDigits";
    public static final String PATTERN = PREFIX + ":pattern";
    public static final String FINAL = PREFIX + ":final";
    public static final String BLOCK = PREFIX + ":block";
    public static final String ABSTRACT = PREFIX + ":abstract";
    public static final String MIXED = PREFIX + ":mixed";
    public static final String NILLABLE = PREFIX + ":nillable";
    public static final String USE = PREFIX + ":use";
    public static final String PROCESS_CONTENTS = PREFIX + ":processContents";
    public static final String FORM = PREFIX + ":form";

    public static final String ID = PREFIX + ":id";

    // Used as names for anonymous types ...
    public static final String COMPLEX_TYPE = PREFIX + ":complexType";
    public static final String SIMPLE_TYPE = PREFIX + ":simpleType";

}
