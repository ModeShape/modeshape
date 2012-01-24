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
         * derived from the node's property values. Note that this field does not include the terms derived from the node's name
         * or path, nor does it include any of the terms for any of the node's binary values (see {@link #BINARY_SHA1S}).
         * 
         * @see BinaryInfoIndex.FieldName#FULL_TEXT
         */
        public static final String FULL_TEXT = "::fts";

        /**
         * The name of the {@link Field string field} used to store the {@link NodeKey}s from all WEAKREFERENCE and REFERENCE
         * properties on the node.
         */
        public static final String WEAK_REFERENCES = "::wref";

        /**
         * The name of the {@link Field string field} used to store the {@link NodeKey}s from all REFERENCE properties on the
         * node.
         */
        public static final String STRONG_REFERENCES = "::sref";

        /**
         * The name of the {@link Field string field} used to store the {@link Binary#getHexHash() SHA-1 hash in hexadecimal form}
         * of all {@link javax.jcr.Binary Binary} values from all BINARY properties on the node.
         * 
         * @see BinaryInfoIndex.FieldName#SHA1
         */
        public static final String BINARY_SHA1S = "::binRef";
    }
}
