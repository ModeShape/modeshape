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
package org.infinispan.schematic;

import java.util.Collection;
import org.infinispan.Cache;
import org.infinispan.lifecycle.Lifecycle;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;

/**
 * A store for JSON documents and other binary content, plus a library of JSON Schema documents used to describe and validate the
 * stored documents.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public interface SchematicDb extends Lifecycle {

    /**
     * Get the name of this database.
     * 
     * @return the library name; never null
     */
    String getName();

    /**
     * Get the cache that backs this schematic database.
     * 
     * @return the Infinispan cache; never null
     */
    Cache<String, SchematicEntry> getCache();

    /**
     * Get the entry with the supplied key.
     *
     * @param key the key or identifier for the document
     * @return the entry, or null if there was no document with the supplied key
     */
    SchematicEntry get( String key );

    /**
     * Get an editor for the entry with the supplied key. This method will automatically try to acquire the lock for this entry.
     * The resulting editor will operate upon a copy of the entry and the database will be updated as part of the transaction. See
     * also {@link #editContent(String, boolean, boolean)} if the lock is known to have already been acquired.
     *
     * @param key the key or identifier for the document
     * @param createIfMissing true if a new entry should be created and added to the database if an existing entry does not exist
     * @return the entry, or null if there was no document with the supplied key
     * @see #editContent(String, boolean, boolean)
     */
    EditableDocument editContent( String key,
                                  boolean createIfMissing );

    /**
     * Get an editor for the entry with the supplied key and say whether the entry should be locked. If {@code acquireLock} is
     * false, then the entry should have been explicitly {@link #lock(Collection) locked} as part of a transaction before this
     * method is called. The resulting editor will operate upon a copy of the entry and the database will be updated as part of
     * the transaction.
     *
     * @param key the key or identifier for the document
     * @param createIfMissing true if a new entry should be created and added to the database if an existing entry does not exist
     * @param acquireLock true if the lock should be acquired for this entry, or false if the lock is known to have already been
     *        acquired for the current transaction
     * @return the entry, or null if there was no document with the supplied key
     */
    EditableDocument editContent( String key,
                                  boolean createIfMissing,
                                  boolean acquireLock );

    /**
     * Determine whether the database contains an entry with the supplied key.
     *
     * @param key the key or identifier for the document
     * @return true if the database contains an entry with this key, or false otherwise
     */
    boolean containsKey( String key );

    /**
     * Store the supplied document and metadata at the given key.
     *
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @return the entry previously stored at this key, or null if there was no entry with the supplied key
     * @see #putIfAbsent(String, Document)
     */
    SchematicEntry put( String key,
                        Document document );

    /**
     * Store the supplied document and metadata at the given key.
     * 
     * @param entryDocument the document that contains the metadata document, content document, and key
     * @return the entry previously stored at this key, or null if there was no entry with the supplied key
     * @see #putIfAbsent(String, Document)
     */
    SchematicEntry put( Document entryDocument );

    /**
     * Store the supplied document and metadata at the given key.
     * 
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @return the existing entry for the supplied key, or null if there was no entry and the put was successful
     */
    SchematicEntry putIfAbsent( String key,
                                Document document );

    /**
     * Replace the existing document and metadata at the given key with the document that is supplied. This method does nothing if
     * there is not an existing entry at the given key.
     *
     * @param key the key or identifier for the document
     * @param document the new document that is to replace the existing document (or binary content)
     * @return the entry that was replaced, or null if nothing was replaced
     */
    SchematicEntry replace( String key,
                            Document document );

    /**
     * Remove the existing document at the given key.
     *
     * @param key the key or identifier for the document
     * @return the entry that was removed, or null if there was no document with the supplied key
     */
    SchematicEntry remove( String key );

    /**
     * Lock all of the documents with the given keys. This must be called within the context of an existing transaction, and all
     * locks will be held until the completion of the transaction.
     *
     * @param key the key for the document that is to be locked
     * @return true if the documents were locked (or if locking is not required), or false if not all of the documents could be
     *         locked
     */
    boolean lock( String key );

    /**
     * Lock all of the documents with the given keys. This must be called within the context of an existing transaction, and all
     * locks will be held until the completion of the transaction.
     *
     * @param keys the set of keys identifying the documents that are to be locked
     * @return true if the documents were locked (or if locking is not required), or false if not all of the documents could be
     *         locked
     */
    boolean lock( Collection<String> keys );
}
