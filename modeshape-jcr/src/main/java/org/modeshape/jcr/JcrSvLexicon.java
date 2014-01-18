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

import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.basic.BasicName;

/**
 * Lexicon of names from the standard JCR "<code>http://www.jcp.org/jcr/sv/1.0</code>" namespace.
 */
@Immutable
public class JcrSvLexicon {

    public static class Namespace {
        public static final String URI = "http://www.jcp.org/jcr/sv/1.0";
        public static final String PREFIX = "sv";
    }

    public static final Name NODE = new BasicName(Namespace.URI, "node");
    public static final Name MULTIPLE = new BasicName(Namespace.URI, "multiple");
    public static final Name PROPERTY = new BasicName(Namespace.URI, "property");
    public static final Name NAME = new BasicName(Namespace.URI, "name");
    public static final Name TYPE = new BasicName(Namespace.URI, "type");
    public static final Name VALUE = new BasicName(Namespace.URI, "value");
}
