/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.schematic;

import java.util.Map;
import org.infinispan.Cache;
import org.infinispan.lifecycle.Lifecycle;
import org.infinispan.schematic.SchemaLibrary.Results;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;
import org.infinispan.util.concurrent.NotifyingFuture;

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
