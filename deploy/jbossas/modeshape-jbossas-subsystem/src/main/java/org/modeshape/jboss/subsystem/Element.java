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
    CACHE_BINARY_STORAGE("cache-binary-storage"),
    COMPOSITE_BINARY_STORAGE("composite-binary-storage"),
    CUSTOM_BINARY_STORAGE("custom-binary-storage"),
    CUSTOM_INDEX_STORAGE("custom-index-storage"),
    DB_BINARY_STORAGE("db-binary-storage"),
    FILE_BINARY_STORAGE("file-binary-storage"),
    INDEXING("indexing"),
    LOCAL_FILE_INDEX_STORAGE("local-file-index-storage"),
    MASTER_FILE_INDEX_STORAGE("master-file-index-storage"),
    NAMED_BINARY_STORE("named-binary-store"),
    PATH_EXPRESSION("path-expression"),
    PROPERTY("property"),
    RAM_INDEX_STORAGE("ram-index-storage"),
    REPOSITORY("repository"),
    WEBAPP("webapp"),
    SEQUENCER("sequencer"),
    SEQUENCERS("sequencers"),
    TEXT_EXTRACTORS("text-extractors"),
    TEXT_EXTRACTOR("text-extractor"),
    SLAVE_FILE_INDEX_STORAGE("slave-file-index-storage"),
    WORKSPACE("workspace"),
    WORKSPACES("workspaces"),
    JOURNALING("journaling"),
    INITIAL_CONTENT("initial-content"),
    NODE_TYPES("node-types"),
    NODE_TYPE("node-type"),
    EXTERNAL_SOURCES("external-sources"),
    SOURCE("source"),
    PROJECTION("projection");

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
