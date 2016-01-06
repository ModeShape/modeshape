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

import java.util.stream.Stream;
import javax.transaction.TransactionManager;
import org.infinispan.Cache;
import org.infinispan.commons.api.Lifecycle;
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
     * Returns a stream over all the keys present in the DB
     * 
     * @return a {@link Stream} instance, never {@code null}
     */
    Stream<String> keys();
    
    /**
     * Get the entry with the supplied key.
     *
     * @param key the key or identifier for the document
     * @return the entry, or null if there was no document with the supplied key
     */
    SchematicEntry get( String key );

    /**
     * Get an editor for the entry with the supplied key. 
     * The resulting editor will operate upon a copy of the entry and the database will be updated as part of the transaction. 
     *
     * @param key the key or identifier for the document
     * @param createIfMissing true if a new entry should be created and added to the database if an existing entry does not exist
     * @return the entry, or null if there was no document with the supplied key
     */
    EditableDocument editContent( String key,
                                  boolean createIfMissing );

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
     * @see #putIfAbsent(String, Document)
     */
    void put( String key,
              Document document );

    /**
     * Store the supplied document and metadata at the given key.
     * 
     * @param entryDocument the document that contains the metadata document, content document, and key
     * @see #putIfAbsent(String, Document)
     */
    void put( Document entryDocument );

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
     * Remove the existing document at the given key.
     *
     * @param key the key or identifier for the document
     * @return the entry that was removed, or null if there was no document with the supplied key
     */
    SchematicEntry remove( String key );
    
    TransactionManager transactionManager();
}
