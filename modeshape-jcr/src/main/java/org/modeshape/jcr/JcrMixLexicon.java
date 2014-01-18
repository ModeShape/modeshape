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
 * Lexicon of names from the standard JCR "<code>http://www.jcp.org/jcr/mix/1.0</code>" namespace.
 */
@Immutable
public class JcrMixLexicon {

    public static class Namespace {
        public static final String URI = "http://www.jcp.org/jcr/mix/1.0";
        public static final String PREFIX = "mix";
    }

    public static final Name REFERENCEABLE = new BasicName(Namespace.URI, "referenceable");
    public static final Name VERSIONABLE = new BasicName(Namespace.URI, "versionable");
    public static final Name LOCKABLE = new BasicName(Namespace.URI, "lockable");
    public static final Name CREATED = new BasicName(Namespace.URI, "created");
    public static final Name LAST_MODIFIED = new BasicName(Namespace.URI, "lastModified");

    /**
     * The name for the "mix:etag" mixin.
     */
    public static final Name ETAG = new BasicName(Namespace.URI, "etag");
    /**
     * The name for the "mix:shareable" mixin.
     */
    public static final Name SHAREABLE = new BasicName(Namespace.URI, "shareable");
    /**
     * The name for the "mix:lifecycle" mixin.
     */
    public static final Name LIFECYCLE = new BasicName(Namespace.URI, "lifecycle");
    /**
     * The name for the "mix:managedRetention" mixin.
     */
    public static final Name MANAGED_RETENTION = new BasicName(Namespace.URI, "managedRetention");

}
