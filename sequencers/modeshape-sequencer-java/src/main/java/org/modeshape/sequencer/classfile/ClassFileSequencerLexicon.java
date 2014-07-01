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

import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.Namespace.PREFIX;

/**
 * The namespace and property names used within a {@link ClassFileSequencer} to store internal information.
 */
public class ClassFileSequencerLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/sequencer/javaclass/1.0";
        public static final String PREFIX = "class";
    }

    public static final String ABSTRACT = PREFIX + ":abstract";
    public static final String ANNOTATION = PREFIX + ":annotation";
    public static final String ANNOTATIONS = PREFIX + ":annotations";
    public static final String METHOD_PARAMETERS = PREFIX + ":methodParameters";
    public static final String ANNOTATION_MEMBER = PREFIX + ":annotationMember";
    public static final String CLASS = PREFIX + ":class";
    public static final String CONSTRUCTORS = PREFIX + ":constructors";
    public static final String ENUM_VALUES = PREFIX + ":enumValues";
    public static final String ENUM = PREFIX + ":enum";
    public static final String FIELD = PREFIX + ":field";
    public static final String FIELDS = PREFIX + ":fields";
    public static final String FINAL = PREFIX + ":final";
    public static final String IMPORTS = PREFIX + ":imports";
    public static final String INTERFACE = PREFIX + ":interface";
    public static final String INTERFACES = PREFIX + ":interfaces";
    public static final String METHOD = PREFIX + ":method";
    public static final String METHODS = PREFIX + ":methods";
    public static final String NAME = PREFIX + ":name";
    public static final String NATIVE = PREFIX + ":native";
    public static final String PACKAGE = PREFIX + ":package";
    public static final String PARAMETERS = PREFIX + ":parameters";
    public static final String PARAMETER = PREFIX + ":parameter";
    public static final String RETURN_TYPE_CLASS_NAME = PREFIX + ":returnTypeClassName";
    public static final String SEQUENCED_DATE = PREFIX + ":sequencedDate";
    public static final String STATIC = PREFIX + ":static";
    public static final String STRICT_FP = PREFIX + ":strictFp";
    public static final String SUPER_CLASS_NAME = PREFIX + ":superClassName";
    public static final String SYNCHRONIZED = PREFIX + ":synchronized";
    public static final String TRANSIENT = PREFIX + ":transient";
    public static final String TYPE_CLASS_NAME = PREFIX + ":typeClassName";
    public static final String VALUE = PREFIX + ":value";
    public static final String VISIBILITY = PREFIX + ":visibility";
    public static final String VOLATILE = PREFIX + ":volatile";

    public static final String ANNOTATION_TYPE = PREFIX + ":annotationType";
    public static final String ANNOTATION_TYPE_MEMBER = PREFIX + ":annotationTypeMember";
    public static final String ANNOTATION_TYPE_MEMBERS = PREFIX + ":annotationTypeMembers";
    public static final String ARGUMENT = PREFIX + ":argument";
    public static final String ARGUMENTS = PREFIX + ":arguments";
    public static final String ARRAY_TYPE = PREFIX + ":arrayType";
    public static final String BASE_TYPE = PREFIX + ":baseType";
    public static final String BODY = PREFIX + ":body";
    public static final String BOUND = PREFIX + ":bound";
    public static final String BOUNDS = PREFIX + ":bounds";
    public static final String BOUND_TYPE = PREFIX + ":boundType";
    public static final String COMMENT = PREFIX + ":comment";
    public static final String COMMENTS = PREFIX + ":comments";
    public static final String COMMENT_TYPE = PREFIX + ":commentType";
    public static final String COMPILATION_UNIT = PREFIX + ":compilationUnit";
    public static final String COMPONENT_TYPE = PREFIX + ":componentType";
    public static final String CONTENT = PREFIX + ":content";
    public static final String DEFAULT = PREFIX + ":default";
    public static final String DIMENSIONS = PREFIX + ":dimensions";
    public static final String DOCUMENTED = PREFIX + ":documented";
    public static final String ENUM_CONSTANT = PREFIX + ":enumConstant";
    public static final String ENUM_CONSTANTS = PREFIX + ":enumConstants";
    public static final String EXPRESSION = PREFIX + ":expression";
    public static final String EXTENDS = PREFIX + ":extends";
    public static final String IMPLEMENTS = PREFIX + ":implements";
    public static final String IMPORT = PREFIX + ":import";
    public static final String INITIALIZER = PREFIX + ":initializer";
    public static final String JAVADOC = PREFIX + ":javadoc";
    public static final String LENGTH = PREFIX + ":length";
    public static final String MESSAGE = PREFIX + ":message";
    public static final String MESSAGES = PREFIX + ":messages";
    public static final String NESTED_TYPES = PREFIX + ":nestedTypes";
    public static final String ON_DEMAND = PREFIX + ":onDemand";
    public static final String PARAMETERIZED_TYPE = PREFIX + ":parameterizedType";
    public static final String PRIMITIVE_TYPE = PREFIX + ":primitiveType";
    public static final String QUALIFIED_TYPE = PREFIX + ":qualifiedType";
    public static final String QUALIFIER = PREFIX + ":qualifier";
    public static final String RETURN_TYPE = PREFIX + ":returnType";
    public static final String SIMPLE_TYPE = PREFIX + ":simpleType";
    public static final String SOURCE_LOCATION = PREFIX + ":sourceLocation";
    public static final String START_POSITION = PREFIX + ":startPosition";
    public static final String STATEMENT = PREFIX + ":statement";
    public static final String STATEMENTS = PREFIX + ":statements";
    public static final String THROWN_EXCEPTIONS = PREFIX + ":thrownExceptions";
    public static final String TYPE = PREFIX + ":type";
    public static final String TYPES = PREFIX + ":types";
    public static final String TYPE_PARAMETER = PREFIX + ":typeParameter";
    public static final String TYPE_PARAMETERS = PREFIX + ":typeParameters";
    public static final String WILDCARD_TYPE = PREFIX + ":wildcardType";
    public static final String VARARGS = PREFIX + ":varargs";

    /**
     * The types of comments.
     */
    public enum CommentType {

        BLOCK,
        JAVADOC,
        LINE

    }

    /**
     * The types of annotations.
     */
    public enum AnnotationType {

        MARKER,
        NORMAL,
        SINGLE_MEMBER

    }

    /**
     * The kind of wildcard type bounds.
     */
    public enum WildcardTypeBound {

        LOWER,
        UPPER

    }

    private ClassFileSequencerLexicon() {
    }
}
