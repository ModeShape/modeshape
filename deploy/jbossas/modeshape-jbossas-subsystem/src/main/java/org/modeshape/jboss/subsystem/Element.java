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
 * Enumerates the XML elements used in the ModeShape subsystem schema.
 */
public enum Element {

    UNKNOWN(null),

    AUTHENTICATORS("authenticators"),
    AUTHENTICATOR("authenticator"),
    COMPOSITE_BINARY_STORAGE("composite-binary-storage"),
    CUSTOM_BINARY_STORAGE("custom-binary-storage"),
    DB_BINARY_STORAGE("db-binary-storage"),
    CASSANDRA_BINARY_STORAGE("cassandra-binary-storage"),
    MONGO_BINARY_STORAGE("mongo-binary-storage"),
    S3_BINARY_STORAGE("s3-binary-storage"),
    FILE_BINARY_STORAGE("file-binary-storage"),
    TRANSIENT_BINARY_STORAGE("transient-binary-storage"),
    INDEX_PROVIDER("index-provider"),
    INDEX_PROVIDERS("index-providers"),
    INDEX("index"),
    INDEXES("indexes"),
    NAMED_BINARY_STORE("named-binary-store"),
    PATH_EXPRESSION("path-expression"),
    PROPERTY("property"),
    REPOSITORY("repository"),
    WEBAPP("webapp"),
    SEQUENCER("sequencer"),
    SEQUENCERS("sequencers"),
    TEXT_EXTRACTORS("text-extractors"),
    TEXT_EXTRACTOR("text-extractor"),
    WORKSPACE("workspace"),
    WORKSPACES("workspaces"),
    JOURNALING("journaling"),
    DB_PERSISTENCE("db-persistence"),
    FILE_PERSISTENCE("file-persistence"),
    PERSISTENCE_TYPE("type"),
    INITIAL_CONTENT("initial-content"),
    NODE_TYPES("node-types"),
    NODE_TYPE("node-type"),
    EXTERNAL_SOURCES("external-sources"),
    SOURCE("source"),
    PROJECTION("projection"),
    REINDEXIG("reindexing");

    private final String name;

    private Element( String name ) {
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

    private static final Map<String, Element> attributes;

    static {
        final Map<String, Element> map = new HashMap<String, Element>();
        for (Element attribute : values()) {
            final String name = attribute.getLocalName();
            if (name != null) map.put(name, attribute);
        }
        attributes = map;
    }

    public static Element forName( String localName ) {
        final Element attribute = attributes.get(localName);
        return attribute == null ? UNKNOWN : attribute;
    }
}
