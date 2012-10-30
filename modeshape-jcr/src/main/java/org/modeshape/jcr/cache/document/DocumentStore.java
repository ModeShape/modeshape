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
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryKey;

/**
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class DocumentStore {

    private final SchematicDb database;
    private String localSourceKey;

    public DocumentStore( SchematicDb database ) {
        this(database, null);
    }

    public DocumentStore( SchematicDb database,
                          String localSourceKey ) {
        this.database = database;
        this.localSourceKey = localSourceKey;
    }

    public DocumentStore localStore() {
       return new DocumentStore(this.database);
    }

    /**
     * Get the entry with the supplied key.
     *
     * @param key the key or identifier for the document
     * @return the entry, or null if there was no document with the supplied key
     */
    public SchematicEntry get( String key ) {
        if (isLocal(key)) {
            return database.get(key);
        }
        return null;
    }


    /**
     * Store the supplied document and metadata at the given key.
     *
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @param metadata the metadata that is to be stored; may be null if there is no metadata
     * @return the existing entry for the supplied key, or null if there was no entry and the put was successful
     */
    public SchematicEntry putIfAbsent( String key,
                                       Document document,
                                       Document metadata ) {
        if (isLocal(key)) {
            return database.putIfAbsent(key, document, metadata);
        }

        return null;
    }


    /**
     * Store the supplied document and metadata at the given key.
     *
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @param metadata the metadata that is to be stored; may be null if there is no metadata
     * @return the entry previously stored at this key, or null if there was no entry with the supplied key
     * @see #putIfAbsent(String, Document, Document)
     */
    public SchematicEntry put( String key,
                               Document document,
                               Document metadata ) {
        if (isLocal(key)) {
            return database.put(key, document, metadata);
        }
        return null;
    }

    /**
     * Store the supplied document in the local db
     *
     * @param entryDocument the document that contains the metadata document, content document, and key
     * @return the entry previously stored at this key, or null if there was no entry with the supplied key
     * @see #putIfAbsent(String, Document, Document)
     */
    public SchematicEntry put( Document entryDocument ) {
        return database.put(entryDocument);
    }

    /**
     * Remove the existing document at the given key.
     *
     * @param key the key or identifier for the document
     * @return the entry that was removed, or null if there was no document with the supplied key
     */
    public SchematicEntry remove( String key ) {
        if (isLocal(key)) {
            return database.remove(key);
        }
        return null;
    }


    /**
     * Determine whether the database contains an entry with the supplied key.
     *
     * @param key the key or identifier for the document
     * @return true if the database contains an entry with this key, or false otherwise
     */
    public boolean containsKey( String key ) {
        if (isLocal(key)) {
            return database.containsKey(key);
        }
        return false;
    }

    /**
     * Replace the existing document and metadata at the given key with the document that is supplied. This method does nothing if
     * there is not an existing entry at the given key.
     *
     * @param key the key or identifier for the document
     * @param document the new document that is to replace the existing document (or binary content)
     * @param metadata the metadata that is to be stored with the replacement document; may be null if there is no metadata for
     * the replacement
     * @return the entry that was replaced, or null if nothing was replaced
     */
    public SchematicEntry replace( String key,
                                   Document document,
                                   Document metadata ) {
        if (isLocal(key)) {
            return database.replace(key, document, metadata);
        }
        return null;
    }

    public TransactionManager transactionManager() {
        return localCache().getAdvancedCache().getTransactionManager();
    }

    public XAResource xaResource() {
        return localCache().getAdvancedCache().getXAResource();
    }

    public Cache<String, SchematicEntry> localCache() {
        return database.getCache();
    }

    public void setLocalSourceKey( String localSourceKey ) {
        this.localSourceKey = localSourceKey;
    }

    private boolean isLocal( String key ) {
        return StringUtil.isBlank(localSourceKey) //there isn't a local source configured yet (e.g. system startup)
                || StringUtil.isBlank(key) //the key is empty - there's no way to tell
                || !NodeKey.isValidFormat(key) //the key isn't a std key format (probably some internal format)
                || key.startsWith(localSourceKey) //the sources differ
                || isLocalBinary(key); //this is half-baked...
    }

    private boolean isLocalBinary( String key ) {
        //TODO author=Horia Chiorean date=10/30/12 description=What about foreign binaries ?
        if (BinaryKey.isProperlyFormattedKey(key)) {
            return true;
        }
        return  (key.endsWith("-ref") && BinaryKey.isProperlyFormattedKey(key.substring(0, key.indexOf("-ref"))));
    }
}
