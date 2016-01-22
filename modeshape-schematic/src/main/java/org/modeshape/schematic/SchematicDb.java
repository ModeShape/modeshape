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
package org.modeshape.schematic;

import java.util.stream.Stream;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;

/**
 * A store for JSON documents and other binary content, plus a library of JSON Schema documents used to describe and validate the
 * stored documents.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @author Horia Chiorean <hchiorea@redhat.com>
 * @since 5.0
 */
public interface SchematicDb extends TransactionListener, Lifecycle {
    
    /**
     * Returns a stream over all the keys present in the DB
     * 
     * @return a {@link Stream} instance, never {@code null}
     */
    Stream<String> keys();
    
    /**
     * Get the document with the supplied key.
     *
     * @param key the key or identifier for the document
     * @return the document, or null if there was no document with the supplied key
     */
    Document get( String key );

    /**
     * Store the supplied document and metadata at the given key. If a document already exists with the given key, this should
     * overwrite the existing document.
     *
     * @param key the key or identifier for the document
     * @param content the document that is to be stored
     * @see #putIfAbsent(String, Document)
     */
    void put( String key, Document content );

    /**
     * Remove the existing document at the given key.
     *
     * @param key the key or identifier for the document
     * @return the document that was removed, or null if there was no document with the supplied key
     */
    Document remove(String key);

    /**
     * Removes all the entries from this DB.
     */
    void removeAll();
    
    /**
     * Get the entry with the supplied key.
     *
     * @param key the key or identifier for the document
     * @return the entry, or null if there was no document with the supplied key
     */
    default SchematicEntry getEntry( String key) {
        Document doc = get(key);
        return doc != null ? () -> get(key) : null;    
    }

    /**
     * Get an editor for the entry with the supplied key. 
     * The resulting editor will operate upon a copy of the entry and the database will be updated as part of the transaction. 
     *
     * @param key the key or identifier for the document
     * @param createIfMissing true if a new entry should be created and added to the database if an existing entry does not exist
     * @return the entry, or null if there was no document with the supplied key and a new one could not be created
     */
    default EditableDocument editContent(String key,
                                         boolean createIfMissing) {
        Document document = get(key);
        if (document != null) {
            return document.editable();
        } else if (createIfMissing) {
            Document empty = SchematicEntry.create(key).source();
            Document existingDocument = putIfAbsent(key, empty);
            return existingDocument != null ? existingDocument.editable() : empty.editable();
        }
        return null;
    }

    /**
     * Determine whether the database contains an entry with the supplied key.
     *
     * @param key the key or identifier for the document
     * @return true if the database contains an entry with this key, or false otherwise
     */
    default boolean containsKey( String key ) {
        return get(key) != null;
    }

    /**
     * Store the supplied document. This document is expected to be a full entry document, which contains both a "metadata"
     * and "content" section.
     * 
     * @param entryDocument the document that contains the metadata document and content document.
     * @see #putIfAbsent(String, Document)
     */
    default void putEntry(Document entryDocument) {
        SchematicEntry entry = () -> entryDocument;
        put(entry.getId(), entry.getContent());
    }

    /**
     * Store the supplied document and metadata at the given key.
     * 
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @return the existing document for the supplied key, or null if there was no entry and the put was successful
     */
    default Document putIfAbsent( String key, Document document ) {
        Document existingDocument = get(key);
        if (existingDocument != null) {
            return existingDocument;
        } else {
            put(key, document);
            return null;
        }
    }
}
