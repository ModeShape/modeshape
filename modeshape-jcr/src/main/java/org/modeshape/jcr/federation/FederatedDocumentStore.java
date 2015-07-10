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
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.SecureHash.Algorithm;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.Connectors;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.document.DocumentStore;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.cache.document.LocalDocumentStore;
import org.modeshape.jcr.cache.document.SessionNode;
import org.modeshape.jcr.spi.federation.Connector;
import org.modeshape.jcr.spi.federation.ConnectorException;
import org.modeshape.jcr.spi.federation.DocumentChanges;
import org.modeshape.jcr.spi.federation.DocumentReader;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.spi.federation.PageKey;
import org.modeshape.jcr.spi.federation.Pageable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.ReferenceFactory;
import org.modeshape.jcr.value.basic.NodeKeyReference;
import org.modeshape.jcr.value.basic.StringReference;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;

/**
 * An implementation of {@link DocumentStore} which is used when federation is enabled
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FederatedDocumentStore implements DocumentStore {

    private static final Logger LOGGER = Logger.getLogger(FederatedDocumentStore.class);

    private static final String FEDERATED_WORKSPACE_KEY = NodeKey.keyForWorkspaceName("federated_ws");

    private final LocalDocumentStore localDocumentStore;
    private final Connectors connectors;
    private DocumentTranslator translator;
    private String localSourceKey;

    /**
     * Creates a new instance with the given connectors and local db.
     * 
     * @param connectors a {@code non-null} {@link Connectors} instance
     * @param localDb a {@code non-null} {@link SchematicDb} instance
     */
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
    public String newDocumentKey( String parentKey,
                                  Name documentName,
                                  Name documentPrimaryType ) {
        if (isLocalSource(parentKey)) {
            return localStore().newDocumentKey(parentKey, documentName, null);
        }
        Connector connector = connectors.getConnectorForSourceKey(sourceKey(parentKey));
        if (connector != null) {
            checkConnectorIsWritable(connector);
            String parentDocumentId = documentIdFromNodeKey(parentKey);
            String newChildId = connector.newDocumentId(parentDocumentId, documentName, documentPrimaryType);
            if (!StringUtil.isBlank(newChildId)) {
                return documentIdToNodeKey(connector.getSourceName(), newChildId).toString();
            }
        }

        return null;
    }

    @Override
    public SchematicEntry storeDocument( String key,
                                         Document document ) {
        if (isLocalSource(key)) {
            return localStore().putIfAbsent(key, document);
        }
        Connector connector = connectors.getConnectorForSourceKey(sourceKey(key));
        if (connector != null) {
            checkConnectorIsWritable(connector);
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
                checkConnectorIsWritable(connector);
                EditableDocument editableDocument = replaceNodeKeysWithDocumentIds(document);
                String documentId = documentIdFromNodeKey(key);
                MutableCachedNode.NodeChanges nodeChanges = sessionNode.getNodeChanges();
                DocumentChanges documentChanges = createDocumentChanges(nodeChanges, connector.getSourceName(), editableDocument,
                                                                        documentId);
                connector.updateDocument(documentChanges);
            }
        }
    }

    private DocumentChanges createDocumentChanges( MutableCachedNode.NodeChanges nodeChanges,
                                                   String sourceName,
                                                   EditableDocument editableDocument,
                                                   String documentId ) {
        FederatedDocumentChanges documentChanges = new FederatedDocumentChanges(documentId, editableDocument);

        // property and mixin changes
        documentChanges.setPropertyChanges(nodeChanges.changedPropertyNames(), nodeChanges.removedPropertyNames());
        documentChanges.setMixinChanges(nodeChanges.addedMixins(), nodeChanges.removedMixins());

        // children
        LinkedHashMap<NodeKey, Name> appendedChildren = nodeChanges.appendedChildren();
        validateSameSourceForAllNodes(sourceName, appendedChildren.keySet());

        Map<NodeKey, LinkedHashMap<NodeKey, Name>> childrenInsertedBefore = nodeChanges.childrenInsertedBefore();
        validateSameSourceForAllNodes(sourceName, childrenInsertedBefore.keySet());
        Map<String, LinkedHashMap<String, Name>> processedChildrenInsertions = new HashMap<String, LinkedHashMap<String, Name>>(
                                                                                                                                childrenInsertedBefore.size());
        for (NodeKey childKey : childrenInsertedBefore.keySet()) {
            LinkedHashMap<NodeKey, Name> insertions = childrenInsertedBefore.get(childKey);
            validateSameSourceForAllNodes(sourceName, insertions.keySet());
            processedChildrenInsertions.put(documentIdFromNodeKey(childKey), nodeKeyMapToIdentifierMap(insertions));
        }

        documentChanges.setChildrenChanges(nodeKeyMapToIdentifierMap(appendedChildren),
                                           nodeKeyMapToIdentifierMap(nodeChanges.renamedChildren()),
                                           nodeKeySetToIdentifiersSet(nodeChanges.removedChildren()), processedChildrenInsertions);

        // parents
        Set<NodeKey> addedParents = nodeChanges.addedParents();

        validateSameSourceForAllNodes(sourceName, addedParents);
        validateNodeKeyHasSource(sourceName, nodeChanges.newPrimaryParent());

        documentChanges.setParentChanges(nodeKeySetToIdentifiersSet(addedParents),
                                         nodeKeySetToIdentifiersSet(nodeChanges.removedParents()),
                                         documentIdFromNodeKey(nodeChanges.newPrimaryParent()));

        // referrers
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

    private void validateNodeKeyHasSource( String sourceName,
                                           NodeKey nodeKey ) {
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        if (nodeKey != null && !sourceKey.equals(nodeKey.getSourceKey())) {
            throw new ConnectorException(JcrI18n.federationNodeKeyDoesNotBelongToSource, nodeKey, sourceName);
        }
    }

    private <T> Map<String, T> nodeKeyMapToIdentifierMap( Map<NodeKey, T> nodeKeysMap ) {
        Map<String, T> result = new HashMap<String, T>(nodeKeysMap.size());
        for (NodeKey key : nodeKeysMap.keySet()) {
            result.put(documentIdFromNodeKey(key), nodeKeysMap.get(key));
        }
        return result;
    }

    private <T> LinkedHashMap<String, T> nodeKeyMapToIdentifierMap( LinkedHashMap<NodeKey, T> nodeKeysMap ) {
        LinkedHashMap<String, T> result = new LinkedHashMap<String, T>(nodeKeysMap.size());
        for (NodeKey key : nodeKeysMap.keySet()) {
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
            Document document = null;
            if (connector instanceof Pageable && PageKey.isValidFormat(key)) {
                // a page was requested
                PageKey pageKey = new PageKey(key);
                String parentId = pageKey.getParentId();
                pageKey = pageKey.withParentId(documentIdFromNodeKey(parentId));
                document = ((Pageable)connector).getChildren(pageKey);
            } else {
                // interpret the key as a regular node id
                String docId = documentIdFromNodeKey(key);
                document = connector.getDocumentById(docId);
            }
            if (document != null) {
                // clone the document, so we don't alter the original
                EditableDocument editableDocument = replaceConnectorIdsWithNodeKeys(document, connector.getSourceName());
                editableDocument = updateCachingTtl(connector, editableDocument);
                editableDocument = updateQueryable(connector, editableDocument);
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

    private EditableDocument updateQueryable( Connector connector,
                                              EditableDocument editableDocument ) {
        if (!connector.isQueryable()) {
            translator.setQueryable(editableDocument, false);
            return editableDocument;
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
            boolean result = localStore().remove(key);
            connectors.internalNodeRemoved(key);
            return result;
        }
        Connector connector = connectors.getConnectorForSourceKey(sourceKey(key));
        if (connector != null) {
            checkConnectorIsWritable(connector);
            boolean result = connector.removeDocument(documentIdFromNodeKey(key));
            connectors.externalNodeRemoved(key);
            return result;
        }
        return false;
    }

    @Override
    public boolean lockDocuments( Collection<String> keys ) {
        return localDocumentStore.lockDocuments(keys);
    }

    @Override
    public EditableDocument edit( String key,
                                  boolean createIfMissing ) {
        return edit(key, createIfMissing, true);
    }

    @Override
    public EditableDocument edit( String key,
                                  boolean createIfMissing,
                                  boolean acquireLock ) {
        if (isLocalSource(key)) {
            return localStore().edit(key, createIfMissing, acquireLock);
        }
        // It's federated, so we have to use the federated logic ...
        FederatedSchematicEntry entry = (FederatedSchematicEntry)get(key);
        if (entry != null) {
            return entry.edit();
        }
        return null;
    }

    @Override
    public TransactionManager transactionManager() {
        return localStore().transactionManager();
    }

    @Override
    public XAResource xaResource() {
        return localStore().xaResource();
    }

    @Override
    public void setLocalSourceKey( String localSourceKey ) {
        this.localSourceKey = localSourceKey;
    }

    @Override
    public String getLocalSourceKey() {
        return this.localSourceKey;
    }

    @Override
    public String createExternalProjection( String projectedNodeKey,
                                            String sourceName,
                                            String externalPath,
                                            String alias ) {
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        Connector connector = connectors.getConnectorForSourceKey(sourceKey);
        if (connector != null) {
            String externalNodeId = connector.getDocumentId(externalPath);
            if (externalNodeId != null) {
                String externalNodeKey = documentIdToNodeKeyString(sourceName, externalNodeId);
                connectors.addProjection(externalNodeKey, projectedNodeKey, alias);
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

    @Override
    public Document getChildReference( String parentKey,
                                       String childKey ) {
        if (isLocalSource(parentKey)) {
            return localStore().getChildReference(parentKey, childKey);
        }
        Connector connector = connectors.getConnectorForSourceKey(sourceKey(parentKey));
        if (connector != null) {
            parentKey = documentIdFromNodeKey(parentKey);
            childKey = documentIdFromNodeKey(childKey);
            Document doc = connector.getChildReference(parentKey, childKey);
            if (doc != null) {
                String key = doc.getString(DocumentTranslator.KEY);
                key = documentIdToNodeKeyString(connector.getSourceName(), key);
                doc = doc.with(DocumentTranslator.KEY, key);
            }
            return doc;
        }
        return null;
    }

    @Override
    public ExternalBinaryValue getExternalBinary( String sourceName,
                                                  String id ) {

        Connector connector = connectors.getConnectorForSourceName(sourceName);
        if (connector == null) {
            LOGGER.debug("Connector not found for source name {0} while trying to get a binary value", sourceName);
            return null;
        }
        return connector.getBinaryValue(id);
    }

    private boolean isLocalSource( String key ) {
        return !NodeKey.isValidFormat(key) // the key isn't a std key format (probably some internal format)
               || StringUtil.isBlank(localSourceKey) // there isn't a local source configured yet (e.g. system startup)
               || key.startsWith(localSourceKey) // the sources differ
               || isBinaryMetadataDocumentKey(key); // it's the metadata document with reference counts for a SHA1 binary value
    }

    private boolean isBinaryMetadataDocumentKey( String key ) {
        return key.endsWith("-ref") && Algorithm.SHA_1.isHexadecimal(key.substring(0, 40)); // 40 hexadecimal characters long
    }

    private String sourceKey( String nodeKey ) {
        return NodeKey.sourceKey(nodeKey);
    }

    private String documentIdToNodeKeyString( String sourceName,
                                              String documentId ) {
        return documentIdToNodeKey(sourceName, documentId).toString();
    }

    static NodeKey documentIdToNodeKey( String sourceName,
                                        String documentId ) {
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        return new NodeKey(sourceKey, FEDERATED_WORKSPACE_KEY, documentId);
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
            externalDocumentKey = documentIdToNodeKeyString(sourceName, externalDocumentId);
            writer.setId(externalDocumentKey);
        }

        // replace the id of each parent and add the optional federated parent
        List<String> parentKeys = new ArrayList<String>();
        for (String parentId : reader.getParentIds()) {
            String parentKey = documentIdToNodeKeyString(sourceName, parentId);
            parentKeys.add(parentKey);
        }

        // replace the id of each block (if they exist)
        EditableDocument childrenInfo = writer.document().getDocument(DocumentTranslator.CHILDREN_INFO);
        if (childrenInfo != null) {
            String nextBlockKey = childrenInfo.getString(DocumentTranslator.NEXT_BLOCK);
            if (!StringUtil.isBlank(nextBlockKey)) {
                childrenInfo.setString(DocumentTranslator.NEXT_BLOCK, documentIdToNodeKeyString(sourceName, nextBlockKey));
            }
            String lastBlockKey = childrenInfo.getString(DocumentTranslator.LAST_BLOCK);
            if (!StringUtil.isBlank(lastBlockKey)) {
                childrenInfo.setString(DocumentTranslator.LAST_BLOCK, documentIdToNodeKeyString(sourceName, lastBlockKey));
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

        // process the properties to look for **INTERNAL** references ...
        for (Property property : reader.getProperties().values()) {
            if (property.isEmpty()) continue;
            if (property.isReference()) {
                if (property.isSingle()) {
                    Object value = convertReferenceValue(property.getFirstValue(), sourceName);
                    writer.addProperty(property.getName(), value);
                } else {
                    assert property.isMultiple();
                    Object[] values = property.getValuesAsArray();
                    for (int i = 0; i != values.length; ++i) {
                        values[i] = convertReferenceValue(values[i], sourceName);
                    }
                    writer.addProperty(property.getName(), values);
                }
            }
        }

        return writer.document();
    }

    private Object convertReferenceValue( Object value,
                                          String sourceName ) {
        if (value instanceof NodeKeyReference) {
            NodeKeyReference ref = (NodeKeyReference)value;
            NodeKey key = ref.getNodeKey();
            NodeKey converted = documentIdToNodeKey(sourceName, key.toString());
            boolean foreign = !converted.getSourceKey().equals(localSourceKey);
            ReferenceFactory factory = ref.isWeak() ? translator.getReferenceFactory() : translator.getReferenceFactory();
            return factory.create(converted, foreign);
        } else if (value instanceof StringReference) {
            StringReference ref = (StringReference)value;
            NodeKey converted = documentIdToNodeKey(sourceName, ref.toString());
            boolean foreign = !converted.getSourceKey().equals(localSourceKey);
            ReferenceFactory factory = ref.isWeak() ? translator.getReferenceFactory() : translator.getReferenceFactory();
            return factory.create(converted, foreign);
        }
        return value;
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

    private void checkConnectorIsWritable( Connector connector ) throws ConnectorException {
        if (connector.isReadonly()) {
            throw new ConnectorException(JcrI18n.connectorIsReadOnly.text(connector.getSourceName()));
        }
    }

}
