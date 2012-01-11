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

import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.Namespace.PREFIX;

/**
 * The namespace and property names used within a {@link ClassFileSequencer} to store internal information.
 */
public class ClassFileSequencerLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/sequencer/javaclass/1.0";
        public static final String PREFIX = "class";
    }

    public static final String ABSTRACT = PREFIX +  ":abstract";
    public static final String ANNOTATION = PREFIX + ":annotation";
    public static final String ANNOTATIONS = PREFIX + ":annotations";
    public static final String ANNOTATION_MEMBER = PREFIX + ":annotationMember";
    public static final String CLASS = PREFIX + ":class";
    public static final String CONSTRUCTORS = PREFIX + ":constructors";
    public static final String ENUM_VALUES = PREFIX +  ":enumValues";
    public static final String ENUM = PREFIX + ":enum";
    public static final String FIELD = PREFIX + ":field";
    public static final String FIELDS = PREFIX + ":fields";
    public static final String FINAL = PREFIX + ":final";
    public static final String INTERFACE = PREFIX + ":interface";
    public static final String INTERFACES = PREFIX + ":interfaces";
    public static final String METHOD = PREFIX + ":method";
    public static final String METHODS = PREFIX + ":methods";
    public static final String NAME = PREFIX + ":name";
    public static final String NATIVE = PREFIX + ":native";
    public static final String PARAMETERS = PREFIX + ":parameters";
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
