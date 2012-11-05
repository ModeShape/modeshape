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

package org.modeshape.jcr.cache.document;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.infinispan.Cache;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;

/**
 * An implementation of {@link DocumentStore} which always uses the local cache to store/retrieve data and which provides
 * some additional methods for exposing local cache information.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class LocalDocumentStore implements DocumentStore {

    private final SchematicDb database;

    /**
     * Creates a new local store with the given database
     *
     * @param database a {@link SchematicDb} instance which must be non-null.
     */
    public LocalDocumentStore( SchematicDb database ) {
        this.database = database;
    }

    @Override
    public boolean containsKey( String key ) {
        return database.containsKey(key);
    }

    @Override
    public SchematicEntry get( String key ) {
        return database.get(key);
    }

    @Override
    public SchematicEntry storeDocument( String key,
                                         Document document ) {
        return putIfAbsent(key, document);
    }

    @Override
    public void updateDocument( String key,
                                Document document ) {
        //do nothing, the way the local store updates is via deltas
    }

    /**
     * @see SchematicDb#putIfAbsent(String, org.infinispan.schematic.document.Document, org.infinispan.schematic.document.Document)
     */
    public SchematicEntry putIfAbsent( String key,
                                       Document document ) {
        return database.putIfAbsent(key, document, null);
    }

    /**
     * @see SchematicDb#put(String, org.infinispan.schematic.document.Document, org.infinispan.schematic.document.Document)
     */
    public void put( String key,
                     Document document ) {
        database.put(key, document, null);
    }

    /**
     * Store the supplied document in the local db
     *
     * @param entryDocument the document that contains the metadata document, content document, and key
     */
    public void put( Document entryDocument ) {
        database.put(entryDocument);
    }

    /**
     * Replace the existing document and metadata at the given key with the document that is supplied. This method does nothing if
     * there is not an existing entry at the given key.
     *
     * @param key the key or identifier for the document
     * @param document the new document that is to replace the existing document (or binary content)
     * the replacement
     */
    public void replace( String key,
                         Document document ) {
        database.replace(key, document, null);
    }

    @Override
    public void remove( String key ) {
        database.remove(key);
    }

    @Override
    public LocalDocumentStore localStore() {
        return this;
    }

    @Override
    public TransactionManager transactionManager() {
        return localCache().getAdvancedCache().getTransactionManager();
    }

    @Override
    public XAResource xaResource() {
        return localCache().getAdvancedCache().getXAResource();
    }

    @Override
    public void setLocalSourceKey( String sourceKey ) {
        //ignore, as this is always local
    }

    @Override
    public EditableDocument getExternalDocumentAtLocation( String sourceName,
                                                           String documentLocation ) {
        throw new UnsupportedOperationException("External documents are not supported in the local document store");
    }

    /**
     * Returns the local Infinispan cache.
     *
     * @return a {@code non-null} {@link Cache} instance.
     */
    public Cache<String, SchematicEntry> localCache() {
        return database.getCache();
    }
}
