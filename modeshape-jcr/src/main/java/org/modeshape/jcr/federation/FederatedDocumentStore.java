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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.Connectors;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.document.DocumentStore;
import org.modeshape.jcr.cache.document.LocalDocumentStore;

/**
 * An implementation of {@link DocumentStore} which is used when federation is enabled
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 *         //TODO author=Horia Chiorean date=11/7/12 description=The externalProjectionKeyToFederatedNodeKey should be marshalled to/from the
 *         system area somehow, at repository shutdown/startup
 */
public class FederatedDocumentStore implements DocumentStore {

    private static final String FEDERATED_WORKSPACE_KEY = NodeKey.keyForWorkspaceName("federated_ws");

    private final LocalDocumentStore localDocumentStore;
    private final Connectors connectorsManager;
    private final Map<String, String> externalProjectionKeyToFederatedNodeKey;

    private String localSourceKey;

    public FederatedDocumentStore( Connectors connectorsManager,
                                   SchematicDb localDb ) {
        this.connectorsManager = connectorsManager;
        this.localDocumentStore = new LocalDocumentStore(localDb);
        this.externalProjectionKeyToFederatedNodeKey = new HashMap<String, String>();
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
        }
        Connector connector = connectorsManager.getConnectorForSourceKey(sourceKey(key));
        if (connector != null) {
            EditableDocument editableDocument = replaceNodeKeysWithDocumentIds(document);
            connector.storeDocument(editableDocument);
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
                EditableDocument editableDocument = replaceNodeKeysWithDocumentIds(document);
                String documentId = documentIdFromNodeKey(key);
                connector.updateDocument(documentId, editableDocument);
            }
        }
    }

    @Override
    public SchematicEntry get( String key ) {
        if (isLocalSource(key)) {
            return localStore().get(key);
        }
        Connector connector = connectorsManager.getConnectorForSourceKey(sourceKey(key));
        if (connector != null) {
            String docId = documentIdFromNodeKey(key);
            Document document = connector.getDocumentById(docId);
            if (document != null) {
                // clone the document, so we don't alter the original
                EditableDocument editableDocument = replaceDocumentIdsWithNodeKeys(document, connector.getSourceName());
                editableDocument = updateCachingTtl(connector, editableDocument);
                return new FederatedSchematicEntry(editableDocument);
            }
        }
        return null;
    }

    private EditableDocument updateCachingTtl( Connector connector,
                                               EditableDocument editableDocument ) {
        Connector.DocumentReader reader = new FederatedDocumentReader(editableDocument);
        //there isn't a specific value set on the document, but the connector has a default value
        if (reader.getCacheTtlSeconds() == null && connector.getCacheTtlSeconds() != null) {
            Connector.DocumentWriter writer = new FederatedDocumentWriter(null, editableDocument);
            writer.setCacheTtlSeconds(connector.getCacheTtlSeconds());
            return writer.document();
        }
        return editableDocument;
    }

    @Override
    public boolean containsKey( String key ) {
        if (isLocalSource(key)) {
            return localStore().containsKey(key);
        }
        Connector connector = connectorsManager.getConnectorForSourceKey(sourceKey(key));
        return connector != null && connector.hasDocument(documentIdFromNodeKey(key));
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

    @Override
    public TransactionManager transactionManager() {
        // TODO author=Horia Chiorean date=11/1/12 description=do we need some other transaction manager ?
        return localStore().transactionManager();
    }

    @Override
    public XAResource xaResource() {
        // TODO author=Horia Chiorean date=11/1/12 description=do we need some other xaResource ?
        return localStore().xaResource();
    }

    @Override
    public void setLocalSourceKey( String localSourceKey ) {
        this.localSourceKey = localSourceKey;
    }

    @Override
    public String createExternalProjection( String federatedNodeKey,
                                            String sourceName,
                                            String externalPath ) {
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        Connector connector = connectorsManager.getConnectorForSourceKey(sourceKey);
        if (connector != null) {
            String documentId = connector.getDocumentId(externalPath);
            if (documentId != null) {
                String nodeKey = documentIdToNodeKey(sourceName, documentId);
                externalProjectionKeyToFederatedNodeKey.put(nodeKey, federatedNodeKey);
                return nodeKey;
            }
        }
        return null;
    }

    private boolean isLocalSource( String key ) {
        return !NodeKey.isValidFormat(key) // the key isn't a std key format (probably some internal format)
                || StringUtil.isBlank(localSourceKey) // there isn't a local source configured yet (e.g. system startup)
                || key.startsWith(localSourceKey); // the sources differ

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

    private EditableDocument replaceDocumentIdsWithNodeKeys( Document externalDocument,
                                                             String sourceName ) {
        Connector.DocumentReader reader = new FederatedDocumentReader(externalDocument);
        Connector.DocumentWriter writer = new FederatedDocumentWriter(null, externalDocument);

        //replace document id with node key
        String sourceDocumentId = reader.getDocumentId();
        assert sourceDocumentId != null;
        String externalDocumentKey = documentIdToNodeKey(sourceName, sourceDocumentId);
        writer.setId(externalDocumentKey);

        //replace the id of each parent and add the optional federated parent
        List<String> parentKeys = new ArrayList<String>();
        for (String parentId : reader.getParentIds()) {
            String parentKey = documentIdToNodeKey(sourceName, parentId);
            parentKeys.add(parentKey);
        }

        String federatedParentKey = externalProjectionKeyToFederatedNodeKey.get(externalDocumentKey);
        if (!StringUtil.isBlank(federatedParentKey)) {
            parentKeys.add(federatedParentKey);
        }
        writer.setParents(parentKeys);

        //process each child in the same way
        List<Document> updatedChildren = new ArrayList<Document>();
        for (EditableDocument child : reader.getChildren()) {
            EditableDocument childWithReplacedIds = replaceDocumentIdsWithNodeKeys(child, sourceName);
            updatedChildren.add(childWithReplacedIds);
        }
        writer.setChildren(updatedChildren);

        return writer.document();
    }

    private EditableDocument replaceNodeKeysWithDocumentIds( Document document ) {
        Connector.DocumentReader reader = new FederatedDocumentReader(document);
        Connector.DocumentWriter writer = new FederatedDocumentWriter(null, document);

        //replace node key with document id
        String documentNodeKey = reader.getDocumentId();
        assert documentNodeKey != null;
        String externalDocumentId = documentIdFromNodeKey(documentNodeKey);
        writer.setId(externalDocumentId);

        //replace the node key with the id of each parent and remove the optional federated parent
        List<String> parentKeys = reader.getParentIds();
        String federatedParentKey = externalProjectionKeyToFederatedNodeKey.get(documentNodeKey);
        if (!StringUtil.isBlank(federatedParentKey)) {
            parentKeys.remove(federatedParentKey);
        }

        List<String> parentIds = new ArrayList<String>();
        for (String parentKey : parentKeys) {
            String parentId = documentIdFromNodeKey(parentKey);
            parentIds.add(parentId);
        }
        writer.setParents(parentIds);

        //process each child in the same way
        List<Document> updatedChildren = new ArrayList<Document>();
        for (EditableDocument child : reader.getChildren()) {
            EditableDocument childWithReplacedIds = replaceNodeKeysWithDocumentIds(child);
            updatedChildren.add(childWithReplacedIds);
        }
        writer.setChildren(updatedChildren);

        return writer.document();
    }

}
