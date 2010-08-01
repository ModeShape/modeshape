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
     * The JCR resource node type (<code>nt:resource</code>).
     */
    String RESOURCE_NODE_TYPE = "nt:resource";

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
    List<String> VALID_QUERY_LANGUAGES = Arrays.asList(new String[] {IJcrConstants.XPATH, IJcrConstants.JCR_SQL,
        IJcrConstants.JCR_SQL2, IJcrConstants.JCR_SEARCH});

}
