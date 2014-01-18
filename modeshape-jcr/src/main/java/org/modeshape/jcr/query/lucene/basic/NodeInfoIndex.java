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
package org.modeshape.jcr.query.lucene.basic;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * The information about the index used to store property value information.
 */
public class NodeInfoIndex {

    /**
     * The name of the index in which the node property values are to be stored.
     */
    public static final String INDEX_NAME = "nodeinfo";

    public static final class FieldName {
        /**
         * The name of the {@link Field string field} in which the node identifier will be placed. The value is always the string
         * form of the {@link NodeKey node key}.
         */
        public static final String ID = "::id";

        /**
         * The name of the {@link Field string field} in which the workspace name will be placed.
         */
        public static final String WORKSPACE = "::wks";

        /**
         * The name of the {@link Field string field} used to store the path of the node in <i>non-standard qualified form</i>,
         * using the <i>durable prefix</i> for the namespace component of each segment. Non-standard form is used, since all path
         * segments include the same-name-sibling (SNS) index, even when the SNS index is "1".
         * <p>
         * The following example shows a path in non-standard qualified form, where "{@code ex}" is the durable prefix for a
         * namespace:
         * 
         * <pre>
         * /ex:foo[1]/ex:bar[2]/child[1]
         * </pre>
         * 
         * Standard and non-standard forms are defined in sections 3.4.3.1 and 3.4.3.2 of the JCR 2.0 specification. Qualified
         * form is defined in section 3.2.5.2 of the JCR 2.0 specification.
         * </p>
         */
        public static final String PATH = "::pth";
        /**
         * The name of the {@link Field string field} used to store the node {@link Name name} in <i>qualified form</i>, using the
         * <i>durable prefix</i> for the namespace used in the name.
         * <p>
         * The following examples shows names in qualified form, where "{@code ex}" is the durable prefix for a namespace:
         * 
         * <pre>
         * ex:foo
         * ex:foo[2]
         * ex:foo[3]
         * other
         * </pre>
         * 
         * Qualified form is defined in section 3.2.5.2 of the JCR 2.0 specification.
         * </p>
         */
        public static final String NODE_NAME = "::nam";
        /**
         * The name of the {@link Field string field} used to store the {@link Name#getLocalName() local name} of the node (which
         * excludes the namespace component). This makes it easier to search for the nodes based upon local name criteria.
         */
        public static final String LOCAL_NAME = "::loc";
        /**
         * The name of the {@link NumericField numeric field} used to store the same-name-sibling index of the node. The values
         * are always positive.
         */
        public static final String SNS_INDEX = "::sns";
        /**
         * The name of the {@link NumericField numeric field} used to store the depth of the node, which is equal to the
         * {@link Path#size() number of segments} in the path, and starts at 0 for the root node.
         */
        public static final String DEPTH = "::dep";

        /**
         * The name of the {@link Field string field} used to store the full-text search analyzed terms for this node, which are
         * derived from the node's property values.
         * 
         * @see NodeInfoIndex.FieldName#FULL_TEXT_PREFIX
         */
        public static final String FULL_TEXT = "::fts";

        /**
         * The prefix of the name of the {@link Field string field} used to store the full-text search analyzed terms of the
         * property's value(s). The remainder of the field name has the form "&lt;namespace>:&lt;local>" (where &lt;namespace> can
         * be zero-length). Note that the prefix uses a single leading ':', which means that it cannot result in the same field
         * name for a property. Also, even if the namespace is zero-length, the free-text search field will be named
         * ":ft::&lt;local>" and will not clash with any other property name.
         * 
         * @see NodeInfoIndex.FieldName#FULL_TEXT
         */
        protected static final String FULL_TEXT_PREFIX = ":ft:";

        /**
         * The prefix of the name of the {@link Field string field} used to store the length of the property value. Note that the
         * prefix uses a single leading ':', which means that it cannot result in the same field name for a property.
         */
        public static final String LENGTH_PREFIX = ":len:";

        /**
         * The prefix of the name of the {@link Field string field} used to store the SHA-1 of the binary values for the property.
         * Note that the prefix uses a single leading ':', which means that it cannot result in the same field name for a
         * property.
         * 
         * @see NodeInfoIndex.FieldName#BINARY_SHA1S
         */
        public static final String BINARY_SHA1_PREFIX = ":sha1:";

        /**
         * The name of the {@link Field string field} used to store the {@link Binary#getHexHash() SHA-1 hash in hexadecimal form}
         * of all {@link javax.jcr.Binary Binary} values from all BINARY properties on the node.
         * 
         * @see NodeInfoIndex.FieldName#BINARY_SHA1_PREFIX
         */
        public static final String BINARY_SHA1S = "::binRef";

        /**
         * The name of the {@link Field string field} used to store the {@link NodeKey}s from all WEAKREFERENCE and REFERENCE
         * properties on the node.
         */
        public static final String ALL_REFERENCES = "::ref";

        /**
         * The name of the {@link Field string field} used to store the {@link NodeKey}s from all REFERENCE properties on the
         * node.
         */
        public static final String STRONG_REFERENCES = "::sref";
    }
}
