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
package org.modeshape.web.jcr.rest.client;

import java.util.Arrays;
import java.util.List;

/**
 * The <code>IJcrContants</code> class provides constants for the commonly used JCR types and property identifiers.
 */
public interface IJcrConstants {

    /**
     * The JCR content property name (<code>jcr:content</code>).
     */
    String CONTENT_PROPERTY = "jcr:content";

    /**
     * The JCR data property name (<code>jcr:data</code>).
     */
    String DATA_PROPERTY = "jcr:data/base64/";

    /**
     * The JCR file node type (<code>nt:file</code>).
     */
    String FILE_NODE_TYPE = "nt:file";

    /**
     * The JCR folder node type (<code>nt:folder</code>).
     */
    String FOLDER_NODE_TYPE = "nt:folder";

    /**
     * The JCR versionable mixin node type (<code>mix:versionable</code>).
     */
    String VERSIONABLE_NODE_TYPE = "mix:versionable";

    /**
     * The JCR data property name (<code>jcr:lastModified</code>).
     */
    String LAST_MODIFIED = "jcr:lastModified";

    /**
     * The JCR data property name (<code>jcr:lastModified</code>).
     */
    String MIME_TYPE = "jcr:mimeType";

    /**
     * The JCR primary type property name (<code>jcr:primaryType</code>).
     */
    String PRIMARY_TYPE_PROPERTY = "jcr:primaryType";

    /**
     * The JCR mixin type property name (<code>jcr:mixinTypes</code>).
     */
    String MIXIN_TYPES_PROPERTY = "jcr:mixinTypes";

    /**
     * The JCR resource node type (<code>nt:resource</code>).
     */
    String RESOURCE_NODE_TYPE = "nt:resource";

    /**
     * The the publish area mixin
     */
    String PUBLISH_AREA_TYPE = "mode:publishArea";

    /**
     * Title property for the publish area mixin
     */
    String PUBLISH_AREA_TITLE = "jcr:title";

    /**
     * Title property for the publish area mixin
     */
    String PUBLISH_AREA_DESCRIPTION = "jcr:description";

    /**
     * The query language value for XPath queries
     */
    String XPATH = "xpath";

    /**
     * The query language value for JCR-SQL queries
     */
    String JCR_SQL = "sql";

    /**
     * The query language value for JCR-SQL2 queries
     */
    String JCR_SQL2 = "JCR-SQL2";

    /**
     * The query language value for full text search queries
     */
    String JCR_SEARCH = "Search";

    /**
     * A list of the valid query languages
     */
    List<String> VALID_QUERY_LANGUAGES = Arrays.asList(IJcrConstants.XPATH, IJcrConstants.JCR_SQL,
                                                       IJcrConstants.JCR_SQL2, IJcrConstants.JCR_SEARCH);

}
