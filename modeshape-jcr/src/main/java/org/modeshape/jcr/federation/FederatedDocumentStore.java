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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.Connectors;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.document.DocumentStore;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.cache.document.LocalDocumentStore;
import org.modeshape.jcr.cache.document.SessionNode;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.federation.spi.ConnectorException;
import org.modeshape.jcr.federation.spi.DocumentChanges;
import org.modeshape.jcr.federation.spi.DocumentReader;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.federation.spi.PageKey;
import org.modeshape.jcr.federation.spi.Pageable;
import org.modeshape.jcr.value.Name;

/**
 * An implementation of {@link DocumentStore} which is used when federation is enabled
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FederatedDocumentStore implements DocumentStore {

    private static final String FEDERATED_WORKSPACE_KEY = NodeKey.keyForWorkspaceName("federated_ws");

    private final LocalDocumentStore localDocumentStore;
    private final Connectors connectors;
    private DocumentTranslator translator;
    private String localSourceKey;

    public FederatedDocumentStore( Connectors connectors,
                                   SchematicDb localDb ) {
        this.connectors = connectors;
        this.localDocumentStore = new LocalDocumentStore(localDb);
    }

    protected final DocumentTranslator translator() {
        if (translator == null) {
            translator = connectors.getDocumentTranslator();
        }
        return translator;
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
        Connector connector = connectors.getConnectorForSourceKey(sourceKey(key));
        if (connector != null) {
            EditableDocument editableDocument = replaceNodeKeysWithDocumentIds(document);
            connector.storeDocument(editableDocument);
        }
        return null;
    }

    @Override
    public void updateDocument( String key,
                                Document document,
                                SessionNode sessionNode ) {
        if (isLocalSource(key)) {
            localStore().updateDocument(key, document, null);
        } else {
            Connector connector = connectors.getConnectorForSourceKey(sourceKey(key));
            if (connector != null) {
                EditableDocument editableDocument = replaceNodeKeysWithDocumentIds(document);
                String documentId = documentIdFromNodeKey(key);
                SessionNode.NodeChanges nodeChanges = sessionNode.getNodeChanges();
                DocumentChanges documentChanges = createDocumentChanges(nodeChanges, connector.getSourceName(), editableDocument, documentId);
                connector.updateDocument(documentChanges);
            }
        }
    }

    private DocumentChanges createDocumentChanges( SessionNode.NodeChanges nodeChanges,
                                                   String sourceName,
                                                   EditableDocument editableDocument,
                                                   String documentId ) {
        FederatedDocumentChanges documentChanges = new FederatedDocumentChanges(documentId, editableDocument);

        //property and mixin changes
        documentChanges.setPropertyChanges(nodeChanges.changedPropertyNames(), nodeChanges.removedPropertyNames());
        documentChanges.setMixinChanges(nodeChanges.addedMixins(), nodeChanges.removedMixins());

        //children
        LinkedHashMap<NodeKey, Name> appendedChildren = nodeChanges.appendedChildren();
        validateSameSourceForAllNodes(sourceName, appendedChildren.keySet());

        Map<NodeKey, LinkedHashMap<NodeKey, Name>> childrenInsertedBefore = nodeChanges.childrenInsertedBefore();
        validateSameSourceForAllNodes(sourceName, childrenInsertedBefore.keySet());
        Map<String,  LinkedHashMap<String, Name>> processedChildrenInsertions = new HashMap<String, LinkedHashMap<String, Name>>(childrenInsertedBefore.size());
        for (NodeKey childKey : childrenInsertedBefore.keySet()) {
            LinkedHashMap<NodeKey, Name> insertions = childrenInsertedBefore.get(childKey);
            validateSameSourceForAllNodes(sourceName, insertions.keySet());
            processedChildrenInsertions.put(documentIdFromNodeKey(childKey), nodeKeyMapToIdentifierMap(insertions));
        }

        documentChanges.setChildrenChanges(nodeKeyMapToIdentifierMap(appendedChildren),
                                           nodeKeyMapToIdentifierMap(nodeChanges.renamedChildren()),
                                           nodeKeySetToIdentifiersSet(nodeChanges.removedChildren()),
                                           processedChildrenInsertions);

        //parents
        Set<NodeKey> addedParents = nodeChanges.addedParents();

        validateSameSourceForAllNodes(sourceName, addedParents);
        validateNodeKeyHasSource(sourceName, nodeChanges.newPrimaryParent());

        documentChanges.setParentChanges(nodeKeySetToIdentifiersSet(addedParents),
                                         nodeKeySetToIdentifiersSet(nodeChanges.removedParents()),
                                         documentIdFromNodeKey(nodeChanges.newPrimaryParent()));

        //referrers
        Set<NodeKey> addedWeakReferrers = nodeChanges.addedWeakReferrers();
        validateSameSourceForAllNodes(sourceName, addedWeakReferrers);

        Set<NodeKey> addedStrongReferrers = nodeChanges.addedStrongReferrers();
        validateSameSourceForAllNodes(sourceName, addedStrongReferrers);

        documentChanges.setReferrerChanges(nodeKeySetToIdentifiersSet(addedWeakReferrers),
                                           nodeKeySetToIdentifiersSet(nodeChanges.removedWeakReferrers()),
                                           nodeKeySetToIdentifiersSet(addedStrongReferrers),
                                           nodeKeySetToIdentifiersSet(nodeChanges.removedStrongReferrers()));

        return documentChanges;
    }

    private void validateSameSourceForAllNodes( String sourceName,
                                                Collection<NodeKey> nodeKeys ) {
        for (NodeKey nodeKey : nodeKeys) {
            validateNodeKeyHasSource(sourceName, nodeKey);
        }
    }

    private void validateNodeKeyHasSource(String sourceName, NodeKey nodeKey) {
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        if (nodeKey != null && !sourceKey.equals(nodeKey.getSourceKey())) {
            throw new ConnectorException(JcrI18n.federationNodeKeyDoesNotBelongToSource, nodeKey, sourceName);
        }
    }

    private <T> Map<String, T> nodeKeyMapToIdentifierMap( Map<NodeKey, T> nodeKeysMap ) {
        Map<String, T> result = new HashMap<String, T>(nodeKeysMap.size());
        for (NodeKey key :  nodeKeysMap.keySet()) {
            result.put(documentIdFromNodeKey(key), nodeKeysMap.get(key));
        }
        return result;
    }

    private <T> LinkedHashMap<String, T> nodeKeyMapToIdentifierMap( LinkedHashMap<NodeKey, T> nodeKeysMap ) {
        LinkedHashMap<String, T> result = new LinkedHashMap<String, T>(nodeKeysMap.size());
        for (NodeKey key :  nodeKeysMap.keySet()) {
            result.put(documentIdFromNodeKey(key), nodeKeysMap.get(key));
        }
        return result;
    }

    private Set<String> nodeKeySetToIdentifiersSet( Set<NodeKey> nodeKeysSet ) {
        Set<String> result = new HashSet<String>(nodeKeysSet.size());
        for (NodeKey key : nodeKeysSet) {
            result.add(documentIdFromNodeKey(key));
        }
        return result;
    }


    @Override
    public SchematicEntry get( String key ) {
        if (isLocalSource(key)) {
            return localStore().get(key);
        }
        Connector connector = connectors.getConnectorForSourceKey(sourceKey(key));
        if (connector != null) {
            String docId = documentIdFromNodeKey(key);
            Document document = connector.getDocumentById(docId);
            if (document != null) {
                // clone the document, so we don't alter the original
                EditableDocument editableDocument = replaceConnectorIdsWithNodeKeys(document, connector.getSourceName());
                editableDocument = updateCachingTtl(connector, editableDocument);
                // Extract any embedded documents ...
                Object removedContainer = editableDocument.remove(DocumentTranslator.EMBEDDED_DOCUMENTS);
                if (removedContainer instanceof EditableDocument) {
                    EditableDocument embeddedDocs = (EditableDocument)removedContainer;
                    for (Document.Field field : embeddedDocs.fields()) {
                        String id = field.getName();
                        Document doc = field.getValueAsDocument();
                        // Place the embedded document in the local value store ...
                        if (doc != null) localStore().put(id, doc);
                    }
                }
                return new FederatedSchematicEntry(editableDocument);
            }
        }
        return null;
    }

    private EditableDocument updateCachingTtl( Connector connector,
                                               EditableDocument editableDocument ) {
        DocumentReader reader = new FederatedDocumentReader(translator(), editableDocument);
        // there isn't a specific value set on the document, but the connector has a default value
        if (reader.getCacheTtlSeconds() == null && connector.getCacheTtlSeconds() != null) {
            DocumentWriter writer = new FederatedDocumentWriter(null, editableDocument);
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
        Connector connector = connectors.getConnectorForSourceKey(sourceKey(key));
        return connector != null && connector.hasDocument(documentIdFromNodeKey(key));
    }

    @Override
    public boolean remove( String key ) {
        if (isLocalSource(key)) {
            return localStore().remove(key);
        }
        Connector connector = connectors.getConnectorForSourceKey(sourceKey(key));
        if (connector != null) {
            return connector.removeDocument(documentIdFromNodeKey(key));
        }
        return false;
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
    public String createExternalProjection( String projectedNodeKey,
                                            String sourceName,
                                            String externalPath ) {
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        Connector connector = connectors.getConnectorForSourceKey(sourceKey);
        if (connector != null) {
            String externalNodeId = connector.getDocumentId(externalPath);
            if (externalNodeId != null) {
                String externalNodeKey = documentIdToNodeKey(sourceName, externalNodeId);
                connectors.mapProjection(externalNodeKey, projectedNodeKey);
                return externalNodeKey;
            }
        }
        return null;
    }

    @Override
    public Document getChildrenBlock( String key ) {
        if (isLocalSource(key)) {
            return localStore().getChildrenBlock(key);
        }
        Connector connector = connectors.getConnectorForSourceKey(sourceKey(key));
        if (connector != null && connector instanceof Pageable) {
            key = documentIdFromNodeKey(key);
            PageKey blockKey = new PageKey(key);
            Document childrenBlock = ((Pageable)connector).getChildren(blockKey);
            if (childrenBlock != null) {
                return replaceConnectorIdsWithNodeKeys(childrenBlock, connector.getSourceName());
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

    private String documentIdFromNodeKey( NodeKey nodeKey ) {
        return nodeKey != null ? nodeKey.getIdentifier() : null;
    }

    private EditableDocument replaceConnectorIdsWithNodeKeys( Document externalDocument,
                                                              String sourceName ) {
        DocumentReader reader = new FederatedDocumentReader(translator(), externalDocument);
        DocumentWriter writer = new FederatedDocumentWriter(translator(), externalDocument);

        // replace document id with node key
        String externalDocumentId = reader.getDocumentId();
        String externalDocumentKey = null;
        if (!StringUtil.isBlank(externalDocumentId)) {
            externalDocumentKey = documentIdToNodeKey(sourceName, externalDocumentId);
            writer.setId(externalDocumentKey);
        }

        // replace the id of each parent and add the optional federated parent
        List<String> parentKeys = new ArrayList<String>();
        for (String parentId : reader.getParentIds()) {
            String parentKey = documentIdToNodeKey(sourceName, parentId);
            parentKeys.add(parentKey);
        }

        // replace the id of each block (if they exist)
        EditableDocument childrenInfo = writer.document().getDocument(DocumentTranslator.CHILDREN_INFO);
        if (childrenInfo != null) {
            String nextBlockKey = childrenInfo.getString(DocumentTranslator.NEXT_BLOCK);
            if (!StringUtil.isBlank(nextBlockKey)) {
                childrenInfo.setString(DocumentTranslator.NEXT_BLOCK, documentIdToNodeKey(sourceName, nextBlockKey));
            }
            String lastBlockKey = childrenInfo.getString(DocumentTranslator.LAST_BLOCK);
            if (!StringUtil.isBlank(lastBlockKey)) {
                childrenInfo.setString(DocumentTranslator.LAST_BLOCK, documentIdToNodeKey(sourceName, lastBlockKey));
            }
        }
        // create the federated node key - external project back reference
        if (externalDocumentKey != null) {
            String projectedNodeKey = connectors.getProjectedNodeKey(externalDocumentKey);
            if (!StringUtil.isBlank(projectedNodeKey)) {
                parentKeys.add(projectedNodeKey);
            }
        }
        writer.setParents(parentKeys);

        // process each child in the same way
        List<Document> updatedChildren = new ArrayList<Document>();
        for (Document child : reader.getChildren()) {
            EditableDocument childWithReplacedIds = replaceConnectorIdsWithNodeKeys(child, sourceName);
            updatedChildren.add(childWithReplacedIds);
        }
        writer.setChildren(updatedChildren);

        return writer.document();
    }

    private EditableDocument replaceNodeKeysWithDocumentIds( Document document ) {
        DocumentReader reader = new FederatedDocumentReader(translator(), document);
        DocumentWriter writer = new FederatedDocumentWriter(translator(), document);

        // replace node key with document id
        String documentNodeKey = reader.getDocumentId();
        assert documentNodeKey != null;
        String externalDocumentId = documentIdFromNodeKey(documentNodeKey);
        writer.setId(externalDocumentId);

        // replace the node key with the id of each parent and remove the optional federated parent
        List<String> parentKeys = reader.getParentIds();
        String projectedNodeKey = connectors.getProjectedNodeKey(documentNodeKey);
        if (!StringUtil.isBlank(projectedNodeKey)) {
            parentKeys.remove(projectedNodeKey);
        }

        List<String> parentIds = new ArrayList<String>();
        for (String parentKey : parentKeys) {
            String parentId = documentIdFromNodeKey(parentKey);
            parentIds.add(parentId);
        }
        writer.setParents(parentIds);

        // process each child in the same way
        List<Document> updatedChildren = new ArrayList<Document>();
        for (Document child : reader.getChildren()) {
            EditableDocument childWithReplacedIds = replaceNodeKeysWithDocumentIds(child);
            updatedChildren.add(childWithReplacedIds);
        }
        writer.setChildren(updatedChildren);

        return writer.document();
    }

}
