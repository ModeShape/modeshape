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

import java.util.Collection;
import java.util.List;
import org.modeshape.schematic.annotation.RequiresTransaction;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;

/**
 * A store for JSON documents and other binary content, plus a library of JSON Schema documents used to describe and validate the
 * stored documents.
 * 
 * @author Horia Chiorean <hchiorea@redhat.com>
 * @since 5.0
 */
public interface SchematicDb extends TransactionListener, Lifecycle, Lockable {

    /**
     * Returns a unique identifier for this schematic DB. 
     * <p>
     * Implementations should make sure that the provided id  is unique per storage instance. 
     * In other words, if two {@link SchematicDb} instances of the same type store data in two different places, they should
     * return a different id.
     * </p>
     * 
     * @return a {@link String}, never {@code null}
     */
    String id();
    
    /**
     * Returns a set over all the keys present in the DB.
     * <p>
     * If this method is called within an existing transaction, it should take into account the transient transactional context
     * (i.e. any local but not yet committed changes)
     * </p>
     * 
     * @return a {@link List} instance, never {@code null}
     */
    List<String> keys();
    
    /**
     * Get the document with the supplied key. This will represent the full {@link SchematicEntry} document if one exists. 
     * <p>
     * If this method is called within an existing transaction, it should take into account the transient transactional context
     * (i.e. any local but not yet committed changes)
     * </p>
     *
     * @param key the key or identifier for the document
     * @return the document, or null if there was no document with the supplied key
     */
    Document get( String key );

    /**
     * Loads a set of documents from the DB returning the corresponding schematic entries.
     * 
     * <p>
     * If this method is called within an existing transaction, it should <b>take into account</b> the transient transactional 
     * context (i.e. any local but not yet committed changes) and either use that (if it exists) or the persisted information.
     * </p>
     * 
     * @param keys an {@link Collection} of keys; never {@code null}
     * @return a {@link List} of {@link SchematicEntry entries}; never {@code null} 
     */
    List<SchematicEntry> load(Collection<String> keys);
    
    /**
     * Stores the supplied schematic entry under the given key. If an entry already exists with the same key, it should be
     * overwritten.
     * @param key a schematic entry id, never {@code null}
     * @param entry a {@link SchematicEntry} instance, never {@code null}
     */
    @RequiresTransaction
    void put(String key, SchematicEntry entry);

    /**
     * Get an editor for the content of the given entry with the supplied key. 
     *
     * @param key the key or identifier for the document
     * @param createIfMissing true if a new entry should be created and added to the database if an existing entry does not exist
     * @return the content document, or null if there was no document with the supplied key and a new one could not be created
     */
    @RequiresTransaction
    EditableDocument editContent(String key, boolean createIfMissing);

    /**
     * Store the supplied content at the given key.
     * 
     * <p>
     *     Depending on the actual implementation, this may or may not be thread-safe. ModeShape never assumes this is thread-safe
     *     when calling it.
     * </p>
     *
     * @param key the key or identifier for the content
     * @param content the content that is to be stored
     * @return the existing entry for the supplied key, or null if there was no entry and the put was successful
     */
    @RequiresTransaction
    SchematicEntry putIfAbsent(String key, Document content);

    /**
     * Remove the existing document at the given key.
     *
     * @param key the key or identifier for the document
     * @return {@code true} if the removal was successful, {@code false} otherwise
     */
    @RequiresTransaction
    boolean remove(String key);

    /**
     * Removes all the entries from this DB.
     */
    @RequiresTransaction
    void removeAll();

    /**
     * Store the supplied content document at the given key. If a document already exists with the given key, this should
     * overwrite the existing document.
     *
     * @param key the key or identifier for the document
     * @param content the document that is to be stored
     * @see #putIfAbsent(String, Document)
     */
    @RequiresTransaction
    default void put( String key, Document content ) {
        put(key, SchematicEntry.create(key, content));
    }

    @Override
    @RequiresTransaction
    default boolean lockForWriting( List<String> locks ) {
        throw new UnsupportedOperationException(getClass() +  " does not support exclusive locking");
    }

    /**
     * Get the entry with the supplied key.
     * <p>
     * If this method is called within an existing transaction, it should take into account the transient transactional context
     * (i.e. any local but not yet committed changes)
     * </p>
     *
     * @param key the key or identifier for the document
     * @return the entry, or null if there was no document with the supplied key
     */
    default SchematicEntry getEntry( String key) {
        Document doc = get(key);
        return doc != null ? () -> doc : null;    
    }
    
    /**
     * Determine whether the database contains an entry with the supplied key.
     * <p>
     * If this method is called within an existing transaction, it should take into account the transient transactional context
     * (i.e. any local but not yet committed changes)
     * </p>
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
    @RequiresTransaction
    default void putEntry(Document entryDocument) {
        SchematicEntry entry = SchematicEntry.fromDocument(entryDocument);
        put(entry.id(), entry);
    }
}
