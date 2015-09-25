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
package org.modeshape.jboss.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumerates the XML attributes used in the ModeShape subsystem schema.
 */
public enum Attribute {

    UNKNOWN(null),

    ACCESS_TYPE("access-type"),
    ALLOW_WORKSPACE_CREATION("allow-workspace-creation"),
    ANALYZER_CLASSNAME("analyzer-classname"),
    ANALYZER_MODULE("analyzer-module"),
    ANONYMOUS_ROLES("anonymous-roles"),
    ANONYMOUS_USERNAME("anonymous-username"),
    CACHE_CONFIG("cache-config"),
    CONFIG_RELATIVE_TO("config-relative-to"),
    CACHE_NAME("cache-name"),
    CLASSNAME("classname"),
    DATA_CACHE_NAME("data-cache-name"),
    DATA_SOURCE_JNDI_NAME("data-source-jndi-name"),
    DEFAULT_WORKSPACE("default-workspace"),
    DOCUMENT_OPTIMIZATION_THREAD_POOL("document-optimization-thread-pool"),
    DOCUMENT_OPTIMIZATION_INITIAL_TIME("document-optimization-initial-time"),
    DOCUMENT_OPTIMIZATION_INTERVAL("document-optimization-interval"),
    DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET("document-optimization-child-count-target"),
    DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE("document-optimization-child-count-tolerance"),
    EVENT_BUS_SIZE("event-bus-size"),
    ENABLE_MONITORING("enable-monitoring"),
    GARBAGE_COLLECTION_THREAD_POOL("garbage-collection-thread-pool"),
    GARBAGE_COLLECTION_INITIAL_TIME("garbage-collection-initial-time"),
    GARBAGE_COLLECTION_INTERVAL("garbage-collection-interval"),
    FORMAT("format"),
    JNDI_NAME("jndi-name"),
    LOCK_CACHE_NAME("lock-cache-name"),
    LOCKING_STRATEGY("locking-strategy"),
    META_CACHE_NAME("metadata-cache-name"),
    CHUNK_SIZE("chunk-size"),
    MIN_VALUE_SIZE("min-value-size"),
    MIN_STRING_SIZE("min-string-size"),
    MIME_TYPE_DETECTION("mime-type-detection"),
    MODULE("module"),
    NAME("name"),
    PATH("path"),
    PATH_EXPRESSION("path-expression"),
    RELATIVE_TO("relative-to"),
    SECURITY_DOMAIN("security-domain"),
    SOURCE_PATH("source-path"),
    SOURCE_RELATIVE_TO("source-relative-to"),
    STORE_NAME("store-name"),
    USE_ANONYMOUS_IF_AUTH_FAILED("use-anonymous-upon-failed-authentication"),
    WORKSPACE_NAMES("workspace-names"),
    CACHE_TTL_SECONDS("cacheTtlSeconds"),
    QUERYABLE("queryable"),
    READONLY("readonly"),
    EXPOSE_AS_WORKSPACE("exposeAsWorkspace"),
    EXPLODED("exploded"),
    MAX_DAYS_TO_KEEP_RECORDS("max-days-to-keep-records"),
    ASYNC_WRITES("async-writes"),
    JOURNALING("journaling"),
    AUTO_DEPLOY("auto-deploy"),
    JOURNAL_GC_THREAD_POOL("journal-gc-thread-pool"),
    JOURNAL_GC_INITIAL_TIME("journal-gc-initial-time"),
    JOURNAL_PATH("journal-path"),
    JOURNAL_RELATIVE_TO("journal-relative-to"),
    INDEX_KIND("kind"),
    SYNCHRONOUS("synchronous"),
    PROVIDER_NAME("provider-name"),
    NODE_TYPE("node-type"),
    THREAD_POOL_NAME("thread-pool-name"),
    MAX_POOL_SIZE("max-pool-size"),
    COLUMNS("columns"),
    REINDEXING_ASNC("async"),
    REINDEXING_MODE("mode");

    private final String name;

    private Attribute( String name ) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Attribute> attributes;

    static {
        final Map<String, Attribute> map = new HashMap<String, Attribute>();
        for (Attribute attribute : values()) {
            final String name = attribute.getLocalName();
            if (name != null) map.put(name, attribute);
        }
        attributes = map;
    }

    public static Attribute forName( String localName ) {
        final Attribute attribute = attributes.get(localName);
        return attribute == null ? UNKNOWN : attribute;
    }
}
