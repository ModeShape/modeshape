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
package org.modeshape.jcr.cache.document;

/**
 * 
 */
public interface DocumentConstants {

    public static final String SHA1 = "sha1";
    public static final String EXTERNAL_BINARY_ID_FIELD = "$externalBinaryId";
    public static final String SOURCE_NAME_FIELD = "$sourceName";
    public static final String SHA1_FIELD = "$sha1";
    public static final String LENGTH = "len";
    public static final String LENGTH_FIELD = "$len";
    public static final String PARENT = "parent";
    public static final String LARGE_VALUE = "value";
    public static final String PROPERTIES = "properties";
    public static final String CHILDREN = "children";
    public static final String CHILDREN_INFO = "childrenInfo";
    public static final String FEDERATED_SEGMENTS = "federatedSegments";
    public static final String COUNT = "count";
    public static final String BLOCK_SIZE = "blockSize";
    public static final String NEXT_BLOCK = "nextBlock";
    public static final String LAST_BLOCK = "lastBlock";
    public static final String NAME = "name";
    public static final String KEY = "key";
    public static final String REFERRERS = "referrers";
    public static final String WEAK = "weak";
    public static final String STRONG = "strong";
    public static final String REFERENCE_COUNT = "refCount";
    public static final String QUERYABLE_FIELD = "$queryable";
    public static final String REFERENCE_FIELD = "$ref";
    public static final String WEAK_REFERENCE_FIELD = "$wref";
    public static final String SIMPLE_REFERENCE_FIELD = "$sref";

    /**
     * A constant that is used as the name for a nested document in which additional, embedded documents can be placed. Each of
     * these documents represents a separate node and will be automatically extracted from the containing document prior to usage.
     */
    public static final String EMBEDDED_DOCUMENTS = "embeddedDocuments";

    /**
     * A constant that can be used by a connector implementation as a supplementary document field, that indicates the maximum
     * number of seconds that particular document should be stored in the workspace cache.
     */
    public static final String CACHE_TTL_SECONDS = "cacheTtlSeconds";

}
