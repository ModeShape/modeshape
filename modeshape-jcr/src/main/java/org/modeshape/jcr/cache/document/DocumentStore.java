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
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;

/**
 * A store which persists/retrieves documents.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface DocumentStore {

    /**
     * Get the entry with the supplied key.
     *
     * @param key the key or identifier for the document
     * @return the entry, or null if there was no document with the supplied key
     */
    public SchematicEntry get( String key );

    /**
     * Store the supplied document at the given key.
     *
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @return the existing entry for the supplied key, or null if there was no entry and the put was successful
     */
    public SchematicEntry putIfAbsent( String key,
                                       Document document );

    /**
     * Store the supplied document and metadata at the given key.
     *
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @see #putIfAbsent(String, Document)
     */
    public void put( String key,
                     Document document );

    /**
     * Store the supplied document in the local db
     *
     * @param entryDocument the document that contains the metadata document, content document, and key
     */
    public void put( Document entryDocument );

    /**
     * Replace the existing document and metadata at the given key with the document that is supplied. This method does nothing if
     * there is not an existing entry at the given key.
     *
     * @param key the key or identifier for the document
     * @param document the new document that is to replace the existing document (or binary content)
     * the replacement
     */
    public void replace( String key,
                         Document document );

    /**
     * Remove the existing document at the given key.
     *
     * @param key the key or identifier for the document
     */
    public void remove( String key );

    /**
     * Determine whether the database contains an entry with the supplied key.
     *
     * @param key the key or identifier for the document
     * @return true if the database contains an entry with this key, or false otherwise
     */
    public boolean containsKey( String key );

    /**
     * Sets the value of the local repository source key.
     *
     * @param sourceKey a {@code non-null} string
     */
    public void setLocalSourceKey( String sourceKey );

    /**
     * Returns a transaction manager instance which can be used to manage transactions for this document store.
     *
     * @return a {@link TransactionManager} instance, never null.
     */
    public TransactionManager transactionManager();

    /**
     * Returns a resource used in distributed transactions
     *
     * @return an {@link XAResource instance} or {@code null}
     */
    public XAResource xaResource();

    /**
     * Returns a local store instance which will use the local Infinispan cache to store/retrieve information.
     *
     * @return a non-null {@link LocalDocumentStore} instance.
     */
    public LocalDocumentStore localStore();
}
