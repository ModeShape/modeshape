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
package org.modeshape.jcr.api;

/**
 * Class which should hold string constants defined by the JCR spec.
 * 
 * @author Horia Chiorean
 */
public final class JcrConstants {
    public static final String JCR_CONTENT = "jcr:content";
    public static final String JCR_ROOT = "jcr:root";
    public static final String JCR_NAME = "jcr:name";
    public static final String JCR_PATH = "jcr:path";
    public static final String JCR_SCORE = "jcr:score";
    public static final String JCR_DATA = "jcr:data";
    public static final String JCR_MIXIN_TYPES = "jcr:mixinTypes";
    public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    public static final String JCR_MIME_TYPE = "jcr:mimeType";

    public static final String NT_UNSTRUCTURED = "nt:unstructured";
    public static final String NT_FOLDER = "nt:folder";
    public static final String NT_FILE = "nt:file";
    public static final String NT_RESOURCE = "nt:resource";

    public static final String MIX_LAST_MODIFIED = "mix:lastModified";
    public static final String MIX_REFERENCEABLE = "mix:referenceable";

    public static final String MODE_LOCAL_NAME = "mode:localName";
    public static final String MODE_DEPTH = "mode:depth";
    public static final String MODE_SHA1 = "mode:sha1";

    private JcrConstants() {
    }
}
