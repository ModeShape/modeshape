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
import java.util.Map;
import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.lifecycle.Lifecycle;
import org.infinispan.schematic.SchemaLibrary.Results;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;

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
     * Get the description of this database.
     * 
     * @return the library description; never null
     */
    String getDescription();

    /**
     * Get the reference to the SchemaLibrary for this database.
     * 
     * @return the schema library; never null
     */
    SchemaLibrary getSchemaLibrary();

    /**
     * Get the cache that backs this schematic database.
     * 
     * @return the Infinispan cache; never null
     */
    Cache<String, SchematicEntry> getCache();

    /**
     * Validate all JSON documents stored within this database, using this database's {@link #getSchemaLibrary() JSON Schema
     * library}. This method works even when the database is distributed, and it blocks until the task is completed.
     * 
     * @return the map of document keys to validation results for all of the JSON documents in the database that are affiliated
     *         with a schema and that had at least one validation error or warning
     */
    Map<String, Results> validateAll();

    /**
     * Execute a Map-Reduce task to validate all JSON documents stored within this database, using this database's
     * {@link #getSchemaLibrary() JSON Schema library}. This method works even when the database is distributed, and it blocks
     * until the task is completed.
     * 
     * @param firstKey the first key of the document that is to be validated
     * @param additionalKeys the additional keys of the documents that are to be validated
     * @return the map of document keys to validation results for all of the JSON documents in the database that are affiliated
     *         with a schema and that had at least one validation error or warning
     */
    Map<String, Results> validate( String firstKey,
                                   String... additionalKeys );

    /**
     * Validate the JSON document store at the specified key.
     * 
     * @param key the key or identifier for the document
     * @return the validation results; or null if there is no JSON document for the given key
     */
    Results validate( String key );

    /**
     * Get the entry with the supplied key.
     * 
     * @param key the key or identifier for the document
     * @return the entry, or null if there was no document with the supplied key
     */
    SchematicEntry get( String key );

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
     * @param metadata the metadata that is to be stored; may be null if there is no metadata
     * @return the entry previously stored at this key, or null if there was no entry with the supplied key
     * @see #put(String, Binary, Document)
     * @see #putIfAbsent(String, Binary, Document)
     * @see #putIfAbsent(String, Document, Document)
     */
    SchematicEntry put( String key,
                        Document document,
                        Document metadata );

    /**
     * Store the supplied binary value and metadata at the given key.
     * 
     * @param key the key or identifier for the document
     * @param binaryContent the binary content that is to be stored
     * @param metadata the metadata that is to be stored; may be null if there is no metadata
     * @return the entry that was previously stored at this key, or null if there was no entry with the supplied key
     */
    SchematicEntry put( String key,
                        Binary binaryContent,
                        Document metadata );

    /**
     * Store the supplied document and metadata at the given key.
     * 
     * @param entryDocument the document that contains the metadata document, content document, and key
     * @return the entry previously stored at this key, or null if there was no entry with the supplied key
     * @see #put(String, Binary, Document)
     * @see #putIfAbsent(String, Binary, Document)
     * @see #putIfAbsent(String, Document, Document)
     */
    SchematicEntry put( Document entryDocument );

    /**
     * Store the supplied document and metadata at the given key.
     * 
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @param metadata the metadata that is to be stored; may be null if there is no metadata
     * @return the existing entry for the supplied key, or null if there was no entry and the put was successful
     */
    SchematicEntry putIfAbsent( String key,
                                Document document,
                                Document metadata );

    /**
     * Store the supplied document and metadata at the given key.
     * 
     * @param key the key or identifier for the document
     * @param binaryContent the binary content that is to be stored
     * @param metadata the metadata that is to be stored; may be null if there is no metadata
     * @return the existing entry for the supplied key, or null if there was no entry and the put was successful
     */
    SchematicEntry putIfAbsent( String key,
                                Binary binaryContent,
                                Document metadata );

    /**
     * Store the supplied document and metadata at the given key.
     * 
     * @param entryDocument the document that contains the metadata document, content document, and key
     * @return the existing entry for the supplied key, or null if there was no entry and the put was successful
     * @see #put(String, Binary, Document)
     * @see #putIfAbsent(String, Binary, Document)
     * @see #putIfAbsent(String, Document, Document)
     */
    SchematicEntry putIfAbsent( Document entryDocument );

    /**
     * Replace the existing document and metadata at the given key with the document that is supplied. This method does nothing if
     * there is not an existing entry at the given key.
     * 
     * @param key the key or identifier for the document
     * @param document the new document that is to replace the existing document (or binary content)
     * @param metadata the metadata that is to be stored with the replacement document; may be null if there is no metadata for
     *        the replacement
     * @return the entry that was replaced, or null if nothing was replaced
     */
    SchematicEntry replace( String key,
                            Document document,
                            Document metadata );

    /**
     * Replace the existing document and metadata at the given key with the document that is supplied. This method does nothing if
     * there is not an existing document at the given key.
     * 
     * @param key the key or identifier for the document
     * @param binaryContent the binary content that is to replace the existing binary content (or document)
     * @param metadata the metadata that is to be stored with the replacement content; may be null if there is no metadata for the
     *        replacement
     * @return the entry that was replaced, or null if nothing was replaced
     */
    SchematicEntry replace( String key,
                            Binary binaryContent,
                            Document metadata );

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
     * @param keys the set of keys identifying the documents that are to be locked
     * @return true if the documents were locked (or if locking is not required), or false if not all of the documents could be
     *         locked
     */
    boolean lock( Collection<String> keys );

    /**
     * Return whether explicit {@link #lock(Collection) locking} is used when editing {@link SchematicEntry#editDocumentContent()
     * document content} or {@link SchematicEntry#editMetadata() metadata}. If this method returns true, then it may be useful to
     * {@link #lock(Collection) preemptively lock} documents that will be modified during a transactions.
     * 
     * @return true if explicit locking is enabled, or false otherwise
     */
    boolean isExplicitLockingEnabled();

    /**
     * Asynchronous version of {@link #get(String)}. This method does not block on remote calls, even if the library cache mode is
     * synchronous.
     * 
     * @param key the key or identifier for the document
     * @return a future containing the entry at the given key; never null
     */
    NotifyingFuture<SchematicEntry> getAsync( String key );

    /**
     * Asynchronous version of {@link #put(String, Document, Document)}. This method does not block on remote calls, even if the
     * library cache mode is synchronous.
     * 
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @param metadata the metadata that is to be stored; may be null if there is no metadata
     * @return a future containing the old document that was previously stored at this key; never null
     */
    NotifyingFuture<SchematicEntry> putAsync( String key,
                                              Document document,
                                              Document metadata );

    /**
     * Asynchronous version of {@link #put(String, Binary, Document)}. This method does not block on remote calls, even if the
     * library cache mode is synchronous.
     * 
     * @param key the key or identifier for the document
     * @param binaryContent the binary content that is to be stored
     * @param metadata the metadata that is to be stored; may be null if there is no metadata
     * @return a future containing the old document that was previously stored at this key; never null
     */
    NotifyingFuture<SchematicEntry> putAsync( String key,
                                              Binary binaryContent,
                                              Document metadata );

    /**
     * Asynchronous version of {@link #putIfAbsent(String, Document, Document)}. This method does not block on remote calls, even
     * 
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @param metadata the metadata that is to be stored; may be null if there is no metadata
     * @return a future containing the existing document, or the null if there was no existing document at the supplied key
     */
    NotifyingFuture<SchematicEntry> putIfAbsentAsync( String key,
                                                      Document document,
                                                      Document metadata );

    /**
     * Asynchronous version of {@link #putIfAbsent(String, Binary, Document)}. This method does not block on remote calls, even
     * 
     * @param key the key or identifier for the document
     * @param binaryContent the binary content that is to be stored
     * @param metadata the metadata that is to be stored; may be null if there is no metadata
     * @return a future containing the existing document, or the null if there was no existing document at the supplied key
     */
    NotifyingFuture<SchematicEntry> putIfAbsentAsync( String key,
                                                      Binary binaryContent,
                                                      Document metadata );

    /**
     * Asynchronous version of {@link #replace(String, Document, Document)}. This method does not block on remote calls, even if
     * the library cache mode is synchronous.
     * 
     * @param key the key or identifier for the document
     * @param document the new document that is to replace the existing document (or binary content)
     * @param metadata the metadata that is to be stored with the replacement document; may be null if there is no metadata for
     *        the replacement
     * @return a future containing the entry that was replaced; never null
     */
    NotifyingFuture<SchematicEntry> replaceAsync( String key,
                                                  Document document,
                                                  Document metadata );

    /**
     * Asynchronous version of {@link #replace(String, Binary, Document)}. This method does not block on remote calls, even if the
     * library cache mode is synchronous.
     * 
     * @param key the key or identifier for the document
     * @param binaryContent the new binary content that is to replace the existing content (or document)
     * @param metadata the metadata that is to be stored with the replacement document; may be null if there is no metadata for
     *        the replacement
     * @return a future containing the entry that was replaced; never null
     */
    NotifyingFuture<SchematicEntry> replaceAsync( String key,
                                                  Binary binaryContent,
                                                  Document metadata );

    /**
     * Asynchronous version of {@link #remove(String)}. This method does not block on remote calls, even if the library cache mode
     * is synchronous.
     * 
     * @param key the key or identifier for the document
     * @return a future containing the old entry that was removed; never null
     */
    NotifyingFuture<SchematicEntry> removeAsync( String key );

}
