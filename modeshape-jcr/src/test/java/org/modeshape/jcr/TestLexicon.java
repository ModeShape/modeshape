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
package org.modeshape.jcr;

import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * Namespace and names for ModeShape testing.
 */
public class TestLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/test/1.0";
        public static final String PREFIX = "modetest";
    }

    public static final Name CONSTRAINED_TYPE = new BasicName(Namespace.URI, "constrainedType");
    public static final Name CONSTRAINED_BINARY = new BasicName(Namespace.URI, "constrainedBinary");
    public static final Name CONSTRAINED_DATE = new BasicName(Namespace.URI, "constrainedDate");
    public static final Name CONSTRAINED_DOUBLE = new BasicName(Namespace.URI, "constrainedDouble");
    public static final Name CONSTRAINED_LONG = new BasicName(Namespace.URI, "constrainedLong");
    public static final Name CONSTRAINED_NAME = new BasicName(Namespace.URI, "constrainedName");
    public static final Name CONSTRAINED_PATH = new BasicName(Namespace.URI, "constrainedPath");
    public static final Name CONSTRAINED_REFERENCE = new BasicName(Namespace.URI, "constrainedReference");
    public static final Name CONSTRAINED_STRING = new BasicName(Namespace.URI, "constrainedString");

    public static final Name MANDATORY_STRING = new BasicName(Namespace.URI, "mandatoryString");
    public static final Name MANDATORY_CHILD = new BasicName(Namespace.URI, "mandatoryChild");

    public static final Name REFERENCEABLE_UNSTRUCTURED = new BasicName(Namespace.URI, "referenceableUnstructured");
    public static final Name NO_SAME_NAME_SIBS = new BasicName(Namespace.URI, "noSameNameSibs");
    public static final Name NODE_WITH_MANDATORY_PROPERTY = new BasicName(Namespace.URI, "nodeWithMandatoryProperty");
    public static final Name NODE_WITH_MANDATORY_CHILD = new BasicName(Namespace.URI, "nodeWithMandatoryChild");

    public static final Name UNORDERABLE_UNSTRUCTURED = new BasicName(Namespace.URI, "unorderableUnstructured");
}
