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
 * Enumerates the XML elements used in the ModeShape subsystem schema.
 */
public enum Element {

    UNKNOWN(null),

    AUTHENTICATORS("authenticators"),
    AUTHENTICATOR("authenticator"),
    CACHE_BINARY_STORAGE("cache-binary-storage"),
    CACHE_INDEX_STORAGE("cache-index-storage"),
    CUSTOM_BINARY_STORAGE("file-binary-storage"),
    CUSTOM_INDEX_STORAGE("custom-index-storage"),
    DB_BINARY_STORAGE("db-binary-storage"),
    FILE_BINARY_STORAGE("file-binary-storage"),
    INDEXING("indexing"),
    LOCAL_FILE_INDEX_STORAGE("local-file-index-storage"),
    MASTER_FILE_INDEX_STORAGE("master-file-index-storage"),
    PATH_EXPRESSION("path-expression"),
    PROPERTY("property"),
    RAM_INDEX_STORAGE("ram-index-storage"),
    REPOSITORY("repository"),
    SEQUENCER("sequencer"),
    SEQUENCERS("sequencers"),
    TEXT_EXTRACTORS("text-extractors"),
    TEXT_EXTRACTOR("text-extractor"),
    SLAVE_FILE_INDEX_STORAGE("slave-file-index-storage"),
    WORKSPACE("workspace"),
    WORKSPACES("workspaces");

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
