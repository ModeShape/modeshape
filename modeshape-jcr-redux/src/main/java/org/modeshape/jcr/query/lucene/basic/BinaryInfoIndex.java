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
import org.modeshape.jcr.cache.NodeKey;

/**
 * The information about the index used to store path and name information.
 */
public class BinaryInfoIndex {

    /**
     * The name of the index in which the binary full-text search terms are to be stored.
     */
    public static final String INDEX_NAME = "binaries";

    public static final class FieldName {
        /**
         * The name of the {@link Field string field} in which the node identifier will be placed. The value is always the string
         * form of the {@link NodeKey node key}.
         * 
         * @see NodeInfoIndex.FieldName#BINARY_SHA1S
         */
        public static final String SHA1 = "::sha1";

        /**
         * The name of the {@link Field string field} used to store the analyzed terms for the text extracted from the binary
         * value.
         * 
         * @see NodeInfoIndex.FieldName#FULL_TEXT
         */
        public static final String FULL_TEXT = "::fts";
    }
}
