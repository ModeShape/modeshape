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
    public static final String BUCKET_ID_LENGTH = "$bucketIdLen";
    public static final String SIZE = "$size";
    public static final String BUCKETS = "$buckets";

    /**
     * A constant that can be used by a connector implementation as a supplementary document field, that indicates the maximum
     * number of seconds that particular document should be stored in the workspace cache.
     */
    public static final String CACHE_TTL_SECONDS = "cacheTtlSeconds";

}
