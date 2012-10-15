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
    ASYNC_THREAD_POOL_SIZE("async-thread-pool-size"),
    ASYNC_MAX_QUEUE_SIZE("async-max-queue-size"),
    BATCH_SIZE("batch-size"),
    CACHE_CONTAINER("cache-container"),
    CACHE_NAME("cache-name"),
    CHANNEL_NAME("channel-name"),
    CHUNK_SIZE("chunk-size"),
    CLASSNAME("classname"),
    CLUSTER_NAME("cluster-name"),
    CLUSTER_STACK("cluster-stack"),
    CONNECTION_FACTORY_JNDI_NAME("connection-factory-jndi-name"),
    COPY_BUFFER_SIZE("copy-buffer-size"),
    DATA_CACHE_NAME("data-cache-name"),
    DATA_SOURCE_JNDI_NAME("data-source-jndi-name"),
    DEFAULT_WORKSPACE("default-workspace"),
    ENABLE_MONITORING("enable-monitoring"),
    FORMAT("format"),
    JNDI_NAME("jndi-name"),
    LOCK_CACHE_NAME("lock-cache-name"),
    LOCKING_STRATEGY("locking-strategy"),
    META_CACHE_NAME("metadata-cache-name"),
    MIN_VALUE_SIZE("min-value-size"),
    MODE("mode"),
    SYSTEM_CONTENT_MODE("systemContentMode"),
    MODULE("module"),
    NAME("name"),
    PATH("path"),
    PATH_EXPRESSION("path-expression"),
    QUEUE_JNDI_NAME("queue-jndi-name"),
    READER_STRATEGY("reader-strategy"),
    REBUILD_UPON_STARTUP("rebuild-upon-startup"),
    REFRESH_PERIOD("refresh-period"),
    RELATIVE_TO("relative-to"),
    RETRY_MARKER_LOOKUP("retry-marker-lookup"),
    RETRY_INIT_PERIOD("retry-initialize-period"),
    SECURITY_DOMAIN("security-domain"),
    SOURCE_PATH("source-path"),
    SOURCE_RELATIVE_TO("source-relative-to"),
    THREAD_POOL("thread-pool"),
    USE_ANONYMOUS_IF_AUTH_FAILED("use-anonymous-upon-failed-authentication"),
    WORKSPACE_NAMES("workspace-names");

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
