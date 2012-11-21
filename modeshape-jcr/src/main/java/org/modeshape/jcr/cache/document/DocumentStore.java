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
import org.modeshape.jcr.cache.DocumentStoreException;

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
     * @throws DocumentStoreException if there is a problem retrieving the document
     */
    public SchematicEntry get( String key );

    /**
     * Store the supplied document at the given key.
     * 
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @return the existing entry for the supplied key, or null if there was no entry and the put was successful
     * @throws DocumentStoreException if there is a problem storing the document
     */
    public SchematicEntry storeDocument( String key,
                                         Document document );

    /**
     * Updates the content of the document at the given key with the given document.
     * 
     *
     * @param key the key or identifier for the document
     * @param document the content with which the existing document should be updated
     * @param sessionNode the {@link SessionNode} instance which contains the changes that caused the update
     * @throws DocumentStoreException if there is a problem updating the document
     */
    public void updateDocument( String key,
                                Document document,
                                SessionNode sessionNode );

    /**
     * Remove the existing document at the given key.
     * 
     * @param key the key or identifier for the document
     * @return true if a document was removed, or false if there was no document with that key
     * @throws DocumentStoreException if there is a problem removing the document
     */
    public boolean remove( String key );

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

    /**
     * Creates an external projection from the federated node with the given key, towards the external node from the given path,
     * from a source.
     * 
     * @param federatedNodeKey a {@code non-null} string, the key of the federated node which will contain the projection
     * @param sourceName a {@code non-null} string, the name of an external source.
     * @param externalPath a {@code non-null} string, representing a path towards a node from the source
     * @return a {@code non-null} string representing the node key of the external node located at {@code externalPath}.
     */
    public String createExternalProjection( String federatedNodeKey,
                                            String sourceName,
                                            String externalPath );

    /**
     * Returns a document representing a block of children, that has the given key.
     * 
     * @param key a {@code non-null} String the key of the block
     * @return either a {@link Document} with children and possibly a pointer to the next block, or {@code null} if there isn't a
     *         block with such a key.
     */
    public Document getChildrenBlock( String key );
}
