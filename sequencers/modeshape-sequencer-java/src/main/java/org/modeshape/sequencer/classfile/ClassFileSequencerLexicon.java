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

    private ClassFileSequencerLexicon() {
    }
}
