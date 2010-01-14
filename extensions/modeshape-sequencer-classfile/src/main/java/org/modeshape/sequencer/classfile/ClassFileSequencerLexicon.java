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

import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * The namespace and property names used within a {@link ClassFileSequencer} to store internal information.
 */
public class ClassFileSequencerLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/sequencer/javaclass/1.0";
        public static final String PREFIX = "class";
    }

    public static final Name ABSTRACT = new BasicName(Namespace.URI, "abstract");
    public static final Name ANNOTATION = new BasicName(Namespace.URI, "annotation");
    public static final Name ANNOTATIONS = new BasicName(Namespace.URI, "annotations");
    public static final Name ANNOTATION_MEMBER = new BasicName(Namespace.URI, "annotationMember");
    public static final Name CLASS = new BasicName(Namespace.URI, "class");
    public static final Name CONSTRUCTORS = new BasicName(Namespace.URI, "constructors");
    public static final Name ENUM_VALUES = new BasicName(Namespace.URI, "enumValues");
    public static final Name ENUM = new BasicName(Namespace.URI, "enum");
    public static final Name FIELD = new BasicName(Namespace.URI, "field");
    public static final Name FIELDS = new BasicName(Namespace.URI, "fields");
    public static final Name FINAL = new BasicName(Namespace.URI, "final");
    public static final Name INTERFACE = new BasicName(Namespace.URI, "interface");
    public static final Name INTERFACES = new BasicName(Namespace.URI, "interfaces");
    public static final Name METHOD = new BasicName(Namespace.URI, "method");
    public static final Name METHODS = new BasicName(Namespace.URI, "methods");
    public static final Name NAME = new BasicName(Namespace.URI, "name");
    public static final Name NATIVE = new BasicName(Namespace.URI, "native");
    public static final Name PARAMETERS = new BasicName(Namespace.URI, "parameters");
    public static final Name RETURN_TYPE_CLASS_NAME = new BasicName(Namespace.URI, "returnTypeClassName");
    public static final Name SEQUENCED_DATE = new BasicName(Namespace.URI, "sequencedDate");
    public static final Name STATIC = new BasicName(Namespace.URI, "static");
    public static final Name STRICT_FP = new BasicName(Namespace.URI, "strictFp");
    public static final Name SUPER_CLASS_NAME = new BasicName(Namespace.URI, "superClassName");
    public static final Name SYNCHRONIZED = new BasicName(Namespace.URI, "synchronized");
    public static final Name TRANSIENT = new BasicName(Namespace.URI, "transient");
    public static final Name TYPE_CLASS_NAME = new BasicName(Namespace.URI, "typeClassName");
    public static final Name VALUE = new BasicName(Namespace.URI, "value");
    public static final Name VISIBILITY = new BasicName(Namespace.URI, "visibility");
    public static final Name VOLATILE = new BasicName(Namespace.URI, "volatile");

}
