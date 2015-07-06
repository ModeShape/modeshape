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

package org.modeshape.jcr.cache.document;

import java.util.Collection;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;

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
     * @param key the key or identifier for the document
     * @param document the content with which the existing document should be updated
     * @param sessionNode the {@link SessionNode} instance which contains the changes that caused the update
     * @throws DocumentStoreException if there is a problem updating the document
     */
    public void updateDocument( String key,
                                Document document,
                                SessionNode sessionNode );

    /**
     * Generates a new key which will be assigned to a new child document when it is being added to its parent.
     *
     * @param parentKey a {@code non-null} {@link String}, the key of the existing parent
     * @param documentName {@code non-null} {@link org.modeshape.jcr.value.Name}, the name of the new child document.
     * @param documentPrimaryType {@code non-null} {@link org.modeshape.jcr.value.Name}, the name of the primary type of the new
     *        child document
     * @return a {@link String} which will be assigned as key to the new child, or {@code null} indicating that no preferred key
     *         is to be used. If this is the case, the repository will assign a random key.
     */
    public String newDocumentKey( String parentKey,
                                  Name documentName,
                                  Name documentPrimaryType );

    /**
     * Attempts to lock all of the documents with the given keys.
     *
     * @param keys the set of keys identifying the documents that are to be updated via
     *        {@link #updateDocument(String, Document, SessionNode)} or via {@link #edit(String,boolean)}.
     * @return true if the documents were locked, or false if not all of the documents could be locked
|    */
    public boolean lockDocuments( Collection<String> keys );

    /**
     * Edit the existing document at the given key after attempting to lock it first.
     *
     * @param key the key or identifier for the document
     * @param createIfMissing true if a new entry should be created and added to the database if an existing entry does not exist
     * @return true if a document was removed, or false if there was no document with that key
     * @throws DocumentStoreException if there is a problem removing the document
     * @throws org.infinispan.util.concurrent.TimeoutException if the lock cannot be acquired within the configured lock acquisition time.
     */
    public EditableDocument edit( String key,
                                  boolean createIfMissing );

    /**
     * Edit the existing document at the given key, and optionally explicitly locking the entry before returning the editor.
     *
     * @param key the key or identifier for the document
     * @param createIfMissing true if a new entry should be created and added to the database if an existing entry does not exist.
     * Implementations are free to ignore this flag.
     * @param acquireLock true if the lock for the entry should be obtained before returning, or false if the lock was already
     *        obtained via {@link #lockDocuments(Collection)} within the current transaction.
     * @return true if a document was removed, or false if there was no document with that key
     * @throws DocumentStoreException if there is a problem removing the document
     * @throws org.infinispan.util.concurrent.TimeoutException if {@code aquireLock} is true and 
     * the lock cannot be acquired within the configured lock acquisition time.
     */
    public EditableDocument edit( String key,
                                  boolean createIfMissing,
                                  boolean acquireLock );

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
     * Returns the value of the local repository source key.
     *
     * @return a {@code non-null} string
     */
    public String getLocalSourceKey();

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
     * @param projectedNodeKey a {@code non-null} string, the key of the federated node which will contain the projection
     * @param sourceName a {@code non-null} string, the name of an external source.
     * @param externalPath a {@code non-null} string, representing a path towards a node from the source
     * @param alias a {@code non-null} string, representing the alias given to the projection.
     * @return a {@code non-null} string representing the node key of the external node located at {@code externalPath}.
     */
    public String createExternalProjection( String projectedNodeKey,
                                            String sourceName,
                                            String externalPath,
                                            String alias );

    /**
     * Returns a document representing a block of children, that has the given key.
     *
     * @param key a {@code non-null} String the key of the block
     * @return either a {@link Document} with children and possibly a pointer to the next block, or {@code null} if there isn't a
     *         block with such a key.
     */
    public Document getChildrenBlock( String key );

    /**
     * Returns a document representing a single child reference from the supplied parent to the supplied child. This method is
     * called when it is too expensive to find the child reference within the child references.
     *
     * @param parentKey the key for the parent
     * @param childKey the key for the child
     * @return the document representation of a child reference, or null if the implementation doesn't support this method or the
     *         parent does not contain a child with the given key
     */
    public Document getChildReference( String parentKey,
                                       String childKey );

    /**
     * Retrieves a binary value which has the given id and which is not stored by ModeShape.
     *
     * @param sourceName a {@code non-null} String; the name of an external source
     * @param id a {@code non-null} String; the id of an external binary value
     * @return either an {@code ExternalBinaryValue} implementation or {@code null}
     */
    public ExternalBinaryValue getExternalBinary( String sourceName,
                                                  String id );
}
