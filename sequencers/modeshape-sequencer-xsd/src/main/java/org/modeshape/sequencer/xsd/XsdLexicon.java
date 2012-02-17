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
package org.modeshape.sequencer.xsd;

import org.modeshape.common.annotation.Immutable;
import static org.modeshape.sequencer.xsd.XsdLexicon.Namespace.PREFIX;


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
