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
package org.modeshape.jcr;

import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.basic.BasicName;

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
