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

package org.modeshape.jcr.federation;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.infinispan.schematic.DocumentFactory;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.internal.document.DocumentEditor;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.document.DocumentStore;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.cache.document.LocalDocumentStore;

/**
 * An implementation of {@link DocumentStore} which is used when federation is enabled
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FederatedDocumentStore implements DocumentStore {

    private static final String FEDERATED_WORKSPACE_KEY = NodeKey.keyForWorkspaceName("federated_ws");

    private final LocalDocumentStore localDocumentStore;
    private final ConnectorsManager connectorsManager;

    private String localSourceKey;

    public FederatedDocumentStore( ConnectorsManager connectorsManager,
                                   SchematicDb localDb ) {
        this.connectorsManager = connectorsManager;
        this.localDocumentStore = new LocalDocumentStore(localDb);
    }

    @Override
    public LocalDocumentStore localStore() {
        return localDocumentStore;
    }

    @Override
    public SchematicEntry storeDocument( String key,
                                         Document document ) {
        if (isLocalSource(key)) {
            return localStore().putIfAbsent(key, document);
        } else {
            Connector connector = connectorsManager.getConnectorForSourceKey(sourceKey(key));
            if (connector != null) {
                EditableDocument editableDocument = DocumentFactory.newDocument(document);
                replaceNodeKeysWithDocumentIds(editableDocument);
                connector.storeDocument(editableDocument);
                return null;
            }
        }
        return null;
    }

    @Override
    public void updateDocument( String key,
                                Document document ) {
        if (isLocalSource(key)) {
            localStore().updateDocument(key, document);
        } else {
            Connector connector = connectorsManager.getConnectorForSourceKey(sourceKey(key));
            if (connector != null) {
                String documentId = documentIdFromNodeKey(key);
                EditableDocument editableDocument = DocumentFactory.newDocument(document);
                replaceNodeKeysWithDocumentIds(editableDocument);
                connector.updateDocument(documentId, editableDocument);
            }
        }
    }

    @Override
    public SchematicEntry get( String key ) {
        if (isLocalSource(key)) {
            return localStore().get(key);
        } else {
            Connector connector = connectorsManager.getConnectorForSourceKey(sourceKey(key));
            if (connector != null) {
                String docId = documentIdFromNodeKey(key);
                EditableDocument document = connector.getDocumentById(docId);
                if (document != null) {
                    //clone the document, so we don't alter the original
                    document = (EditableDocument)document.clone();
                    replaceDocumentIdsWithNodeKeys(document, connector.getSourceName());
                    return new FederatedSchematicEntry(document);
                }
            }
        }
        return null;
    }

    @Override
    public boolean containsKey( String key ) {
        if (isLocalSource(key)) {
            return localStore().containsKey(key);
        } else {
            Connector connector = connectorsManager.getConnectorForSourceKey(sourceKey(key));
            return connector != null && connector.hasDocument(documentIdFromNodeKey(key));
        }
    }

    @Override
    public void remove( String key ) {
        if (isLocalSource(key)) {
            localStore().remove(key);
        } else {
            Connector connector = connectorsManager.getConnectorForSourceKey(sourceKey(key));
            if (connector != null) {
                connector.removeDocument(documentIdFromNodeKey(key));
            }
        }
    }

    public TransactionManager transactionManager() {
        //TODO author=Horia Chiorean date=11/1/12 description=do we need some other transaction manager ?
        return localStore().transactionManager();
    }

    public XAResource xaResource() {
        //TODO author=Horia Chiorean date=11/1/12 description=do we need some other xaResource ?
        return localStore().xaResource();
    }

    public void setLocalSourceKey( String localSourceKey ) {
        this.localSourceKey = localSourceKey;
    }

    @Override
    public EditableDocument getExternalDocumentAtLocation( String sourceName,
                                                           String documentLocation ) {
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        Connector connector = connectorsManager.getConnectorForSourceKey(sourceKey);
        EditableDocument document = null;
        if (connector != null) {
            document = connector.getDocumentAtLocation(documentLocation);
            if (document != null) {
                //clone the original so we don't overwrite the values from the connector
                document = (EditableDocument)document.clone();
                replaceDocumentIdsWithNodeKeys(document, sourceName);
            }
        }
        return document;
    }

    @Override
    public void setParent( String federatedNodeKey,
                           String documentKey ) {
        Connector connector = connectorsManager.getConnectorForSourceKey(sourceKey(documentKey));
        if (connector != null) {
            String documentId = documentIdFromNodeKey(documentKey);
            connector.setParent(federatedNodeKey, documentId);
        }
    }

    private boolean isLocalSource( String key ) {
        return !NodeKey.isValidFormat(key) //the key isn't a std key format (probably some internal format)
                || StringUtil.isBlank(localSourceKey) //there isn't a local source configured yet (e.g. system startup)
                || key.startsWith(localSourceKey); //the sources differ

    }

    private String sourceKey( String nodeKey ) {
        return NodeKey.sourceKey(nodeKey);
    }

    private String documentIdToNodeKey( String sourceName,
                                        String documentId ) {
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        return new NodeKey(sourceKey, FEDERATED_WORKSPACE_KEY, documentId).toString();
    }

    private String documentIdFromNodeKey( String nodeKey ) {
        return new NodeKey(nodeKey).getIdentifier();
    }

    private void replaceDocumentIdsWithNodeKeys( EditableDocument document,
                                                 String sourceName ) {
        if (document.containsField(DocumentTranslator.KEY)) {
            String sourceDocumentId = document.getString(DocumentTranslator.KEY);
            document.setString(DocumentTranslator.KEY, documentIdToNodeKey(sourceName, sourceDocumentId));
        }
        if (document.containsField(DocumentTranslator.CHILDREN)) {
            EditableArray children = document.getArray(DocumentTranslator.CHILDREN);
            for (Object child : children) {
                assert child instanceof MutableDocument;
                EditableDocument editableChild = new DocumentEditor((MutableDocument)child);
                replaceDocumentIdsWithNodeKeys(editableChild, sourceName);
            }
        }
        if (document.containsField(DocumentTranslator.PARENT)) {
            String parentKey = document.getString(DocumentTranslator.PARENT);
            //check to see if the parent key is that of a federated node and if so, do nothing
            if (NodeKey.isValidFormat(parentKey) && NodeKey.sourceKey(parentKey).equalsIgnoreCase(localSourceKey)) {
                return;
            }
            document.setString(DocumentTranslator.PARENT, documentIdToNodeKey(sourceName, parentKey));
        }
    }

    private void replaceNodeKeysWithDocumentIds( EditableDocument editableDocument) {
        if (editableDocument.containsField(DocumentTranslator.KEY)) {
            String nodeKey = editableDocument.getString(DocumentTranslator.KEY);
            editableDocument.setString(DocumentTranslator.KEY, documentIdFromNodeKey(nodeKey));
        }
        if (editableDocument.containsField(DocumentTranslator.CHILDREN)) {
            EditableArray children = editableDocument.getArray(DocumentTranslator.CHILDREN);
            for (Object child : children) {
                assert child instanceof MutableDocument;
                EditableDocument editableChild = new DocumentEditor((MutableDocument)child);
                replaceNodeKeysWithDocumentIds(editableChild);
            }
        }
        if (editableDocument.containsField(DocumentTranslator.PARENT)) {
            String nodeKey = editableDocument.getString(DocumentTranslator.PARENT);
            editableDocument.setString(DocumentTranslator.PARENT, documentIdFromNodeKey(nodeKey));
        }
    }
}
