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
import java.util.List;
import java.util.Set;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.modeshape.schematic.SchematicEntry;
import org.modeshape.schematic.annotation.RequiresTransaction;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;

/**
 * A store which persists/retrieves documents in a JCR context.
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
     * 
     * @see org.modeshape.schematic.SchematicDb#get(String) 
     */
    public SchematicEntry get( String key );

    /**
     * Loads a set of entries from the document store. This should always return the latest persisted view of the entries.
     * 
     * @param keys a {@link Set} of document keys; may not be null
     * @return a {@link Collection} of {@link SchematicEntry entries}; never {@code null} 
     */
    public List<SchematicEntry> load(Collection<String> keys);

    /**
     * Store the supplied document at the given key.
     *
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @return the existing entry for the supplied key, or null if there was no entry and the put was successful
     * @throws DocumentStoreException if there is a problem storing the document
     * 
     * @see org.modeshape.schematic.SchematicDb#putIfAbsent(String, Document) 
     */
    @RequiresTransaction
    public SchematicEntry storeIfAbsent(String key, Document document);

    /**
     * Updates the content of the document at the given key with the given document.
     *
     * @param key the key or identifier for the document
     * @param document the content with which the existing document should be updated
     * @param sessionNode the {@link SessionNode} instance which contains the changes that caused the update
     * @throws DocumentStoreException if there is a problem updating the document
     */
    @RequiresTransaction
    public void updateDocument( String key, Document document, SessionNode sessionNode );

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
     * <p>
     * NOTE: This should only be called within an existing transaction. If this operation succeeds, all the locked keys will
     * be released automatically when the transaction completes (regardless whether successfully or not).
     * </p>
     *
     * @param keys the set of keys identifying the documents that are to be updated via
     *        {@link #updateDocument(String, Document, SessionNode)} or via {@link #edit(String,boolean)}.
     * @return true if the documents were locked, or false if not all of the documents could be locked
     * @throws IllegalStateException if no active transaction can be detected when the locking is attempted
|    */
    @RequiresTransaction
    public boolean lockDocuments( Collection<String> keys );   
    
    /**
     * Attempts to lock all of the documents with the given keys.
     * <p>
     * NOTE: This should only be called within an existing transaction. If this operation succeeds, all the locked keys will
     * be released automatically when the transaction completes (regardless whether successfully or not)
     * </p>
         
     * @param keys the set of keys identifying the documents that are to be updated via
     *        {@link #updateDocument(String, Document, SessionNode)} or via {@link #edit(String,boolean)}.
     * @return true if the documents were locked, or false if not all of the documents could be locked
     * @throws IllegalStateException if no active transaction can be detected when the locking is attempted
    */
    @RequiresTransaction
    public boolean lockDocuments( String... keys );

    /**
     * Edit the existing document at the given key. 
     * <p>
     *     NOTE: This method does not perform any locking on that key. As such, the caller code should make sure 
     *     {@link #lockDocuments} is called first on all the keys that are about to be changed if the operation
     *     can be performed from a concurrent context.
     * </p>
     *
     * @param key the key or identifier for the document
     * @param createIfMissing true if a new entry should be created and added to the database if an existing entry does not exist
     * @return a {@link EditableDocument} instance if either a document exists at the given key or a new one was created and added
     * successfully. If a document does not already exist and cannot be created, then this will return {@code null} 
     * 
     * @see org.modeshape.schematic.SchematicDb#editContent(String, boolean) 
     */
    @RequiresTransaction
    public EditableDocument edit( String key, boolean createIfMissing );

    /**
     * Remove the existing document at the given key.
     *
     * <p>
     *     NOTE: This method does not perform any locking on that key. As such, the caller code should make sure 
     *     {@link #lockDocuments} is called first on all the keys that are about to be changed if the operation
     *     can be performed from a concurrent context.
     * </p>
     *
     * @param key the key or identifier for the document
     * @return true if a document was removed, or false if there was no document with that key
     * @throws DocumentStoreException if there is a problem removing the document
     * 
     * @see org.modeshape.schematic.SchematicDb#remove(String) 
     */
    @RequiresTransaction
    public boolean remove( String key );

    /**
     * Determine whether the database contains an entry with the supplied key.
     *
     * @param key the key or identifier for the document
     * @return true if the database contains an entry with this key, or false otherwise
     * 
     * @see org.modeshape.schematic.SchematicDb#containsKey(String) 
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
     * Returns a local store instance which is used to persist internal repository information.
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
                                            String alias,
                                            SessionCache systemSession);
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
