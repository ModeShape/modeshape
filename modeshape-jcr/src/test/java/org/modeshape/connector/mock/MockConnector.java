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
package org.modeshape.connector.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.NamespaceRegistry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.JcrNtLexicon;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.federation.spi.DocumentChanges;
import org.modeshape.jcr.federation.spi.DocumentReader;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.federation.spi.PageKey;
import org.modeshape.jcr.federation.spi.Pageable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * Mock implementation of a {@code Connector} which uses some hard-coded, in-memory external nodes to validate the Connector SPI.
 */
public class MockConnector extends Connector implements Pageable {
    public static final String SOURCE_NAME = "mock-source";
    public static final String DOC1_LOCATION = "/doc1";
    public static final String DOC2_LOCATION = "/doc2";

    static final String PAGED_DOC_LOCATION = "/pagedDoc";

    private static final String DOC3_LOCATION = DOC2_LOCATION + "/doc3";

    private final String PAGED_DOCUMENT_ID = newId();

    private final static Map<String, Document> persistentDocumentsByLocation = new LinkedHashMap<String, Document>();
    private final static Map<String, Document> persistentDocumentsById = new HashMap<String, Document>();

    private final Map<String, Document> documentsByLocation = new LinkedHashMap<String, Document>();
    private final Map<String, Document> documentsById = new HashMap<String, Document>();

    /**
     * Flag which allow the mock data to be the same across repository restarts (i.e. connector re-initialization). This is
     * set via reflection from the configuration file.
     */
    private boolean persistentDataAcrossRestarts = false;

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) {
        boolean alreadyInitialized = !persistentDocumentsById.isEmpty() || !persistentDocumentsByLocation.isEmpty();

        if (!alreadyInitialized || !persistentDataAcrossRestarts) {
            String id1 = newId();
            EditableDocument doc1 = newDocument(id1).setPrimaryType(JcrNtLexicon.UNSTRUCTURED)
                    .addProperty(nameFrom("federated1_prop1"), "a string")
                    .addProperty("federated1_prop2", 12)
                    .document();
            documentsByLocation.put(DOC1_LOCATION, doc1);
            documentsById.put(id1, doc1);

            String id2 = newId();
            String id3 = newId();
            EditableDocument doc3 = newDocument(id3).setPrimaryType(JcrNtLexicon.UNSTRUCTURED)
                    .addProperty("federated3_prop1", "yet another string")
                    .setParent(id2)
                    .document();
            documentsById.put(id3, doc3);
            documentsByLocation.put(DOC3_LOCATION, doc3);

            EditableDocument doc2 = newDocument(id2).setPrimaryType(JcrNtLexicon.UNSTRUCTURED)
                    .addProperty("federated2_prop1", "another string")
                    .addProperty("federated2_prop2", Boolean.FALSE)
                    .addChild(id3, "federated3")
                    .document();
            documentsByLocation.put(DOC2_LOCATION, doc2);
            documentsById.put(id2, doc2);

            String id4 = newId();
            EditableDocument doc4 = newDocument(id4).setPrimaryType(JcrNtLexicon.UNSTRUCTURED)
                    .setParent(PAGED_DOCUMENT_ID)
                    .document();
            documentsById.put(id4, doc4);

            String id5 = newId();
            EditableDocument doc5 = newDocument(id5).setPrimaryType(JcrNtLexicon.UNSTRUCTURED)
                    .setParent(PAGED_DOCUMENT_ID)
                    .document();
            documentsById.put(id5, doc5);

            String id6 = newId();
            EditableDocument doc6 = newDocument(id6).setPrimaryType(JcrNtLexicon.UNSTRUCTURED)
                    .setParent(PAGED_DOCUMENT_ID)
                    .document();
            documentsById.put(id6, doc6);

            EditableDocument pagedDoc = newDocument(PAGED_DOCUMENT_ID).setPrimaryType(JcrNtLexicon.UNSTRUCTURED)
                    .addChild(id4, "federated4")
                    .addChild(id5, "federated5")
                    .addChild(id6, "federated6")
                    .document();
            documentsById.put(PAGED_DOCUMENT_ID, pagedDoc);
            documentsByLocation.put(PAGED_DOC_LOCATION, pagedDoc);
        }

        if (!alreadyInitialized) {
            //store the static data only once
            persistentDocumentsByLocation.putAll(documentsByLocation);
            persistentDocumentsById.putAll(documentsById);
        }

        if (persistentDataAcrossRestarts) {
            //make sure the same data that was created when first initialization was performed is saved
            documentsByLocation.clear();
            documentsByLocation.putAll(persistentDocumentsByLocation);
            documentsById.clear();
            documentsById.putAll(persistentDocumentsById);
        }
    }

    @Override
    public Document getDocumentById( String id ) {
        Document doc = documentsById.get(id);
        // this is hard-coded in order to be able to validate the paging mechanism
        if (PAGED_DOCUMENT_ID.equals(id)) {
            DocumentReader reader = readDocument(doc);
            List<? extends Document> children = reader.getChildren();
            DocumentWriter writer = newDocument(id).setPrimaryType(JcrNtLexicon.UNSTRUCTURED)
                    .setChildren(children.subList(0, 1))
                    .addPage(reader.getDocumentId(), 1, 1, children.size());
            return writer.document();
        }
        return doc;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public String getDocumentId( String path ) {
        Document document = documentsByLocation.get(path);
        return document != null ? readDocument(document).getDocumentId() : null;
    }

    @Override
    public boolean removeDocument( String id ) {
        Document doc = documentsById.remove(id);
        if (doc != null) {
            for (Iterator<Map.Entry<String, Document>> iterator = documentsByLocation.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, Document> entry = iterator.next();
                if (entry.getValue().equals(doc)) {
                    iterator.remove();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasDocument( String id ) {
        return documentsById.containsKey(id);
    }

    @Override
    public void storeDocument( Document document ) {
        DocumentReader reader = readDocument(document);
        String documentId = reader.getDocumentId();
        assert documentId != null;
        documentsById.put(documentId, document);
    }

    @Override
    public void updateDocument( DocumentChanges documentChanges ) {
        String id = documentChanges.getDocumentId();
        if (!documentsById.containsKey(id)) {
            return;
        }
        Document existingDocument = documentsById.get(id);
        DocumentReader existingDocumentReader = readDocument(existingDocument);

        DocumentWriter updatedDocumentWriter = newDocument(id);
        updateProperties(existingDocumentReader, updatedDocumentWriter, documentChanges);
        updateMixins(existingDocumentReader, updatedDocumentWriter, documentChanges);
        updateChildren(existingDocumentReader, updatedDocumentWriter, documentChanges);
        updateParents(existingDocumentReader, updatedDocumentWriter, documentChanges);

        documentsById.put(id, updatedDocumentWriter.document());
    }

    private void updateParents( DocumentReader existingDocumentReader,
                                DocumentWriter updatedDocumentWriter,
                                DocumentChanges documentChanges ) {
        DocumentChanges.ParentChanges parentChanges = documentChanges.getParentChanges();
        List<String> parentIds = new ArrayList<String>(existingDocumentReader.getParentIds());

        if (parentChanges.hasNewPrimaryParent()) {
            parentIds.clear();
            parentIds.add(parentChanges.getNewPrimaryParent());
        }

        parentIds.removeAll(parentChanges.getRemoved());
        parentIds.addAll(parentChanges.getAdded());

        updatedDocumentWriter.setParents(parentIds);
    }

    private void updateMixins( DocumentReader existingDocumentReader,
                               DocumentWriter updatedDocumentWriter,
                               DocumentChanges documentChanges ) {
        DocumentChanges.MixinChanges mixinChanges = documentChanges.getMixinChanges();
        Set<Name> mixins = new HashSet<Name>(existingDocumentReader.getMixinTypes());
        mixins.removeAll(mixinChanges.getRemoved());
        mixins.addAll(mixinChanges.getAdded());

        for (Name mixinName : mixins) {
            updatedDocumentWriter.addMixinType(mixinName);
        }
    }

    private void updateChildren( DocumentReader existingDocumentReader,
                                 DocumentWriter updatedDocumentWriter,
                                 DocumentChanges documentChanges ) {
        DocumentChanges.ChildrenChanges childrenChanges = documentChanges.getChildrenChanges();
        LinkedHashMap<String, Name> childrenMap = existingDocumentReader.getChildrenMap();


        //process renames and appended
        childrenMap.putAll(childrenChanges.getRenamed());
        childrenMap.putAll(childrenChanges.getAppended());

        //process removals
        for (String removedChildId : childrenChanges.getRemoved()) {
            childrenMap.remove(removedChildId);
        }

        List<String> childrenIdsList = new ArrayList<String>(childrenMap.keySet());

        if (!childrenChanges.getInsertedBeforeAnotherChild().isEmpty()) {
            Map<String, LinkedHashMap<String, Name>> insertedBeforeAnotherChild = childrenChanges.getInsertedBeforeAnotherChild();

            for (String insertedBefore : insertedBeforeAnotherChild.keySet()) {
                List<String> insertedChildren = new ArrayList<String>(insertedBeforeAnotherChild.get(
                        insertedBefore).keySet());
                for (String insertedChild : insertedChildren) {
                    Collections.swap(childrenIdsList, childrenIdsList.indexOf(insertedBefore),
                                     childrenIdsList.indexOf(insertedChild));
                }
            }

            LinkedHashMap<String, Name> reorderedChildMap = new LinkedHashMap<String, Name>();
            for (String childId : childrenIdsList) {
                reorderedChildMap.put(childId, childrenMap.get(childId));
            }
            childrenMap = reorderedChildMap;
        }


        updatedDocumentWriter.setChildren(childrenMap);
    }

    private void updateProperties( DocumentReader existingDocumentReader,
                                   DocumentWriter updatedDocumentWriter,
                                   DocumentChanges documentChanges ) {
        DocumentChanges.PropertyChanges propertyChanges = documentChanges.getPropertyChanges();
        Map<Name, Property> properties = existingDocumentReader.getProperties();
        DocumentReader updatedDocumentReader = readDocument(documentChanges.getDocument());

        // additions
        for (Name changedPropertyName : propertyChanges.getAdded()) {
            properties.put(changedPropertyName, updatedDocumentReader.getProperty(changedPropertyName));
        }

        // changes
        for (Name changedPropertyName : propertyChanges.getChanged()) {
            properties.put(changedPropertyName, updatedDocumentReader.getProperty(changedPropertyName));
        }

        //removals
        for (Name removedPropertyName : propertyChanges.getRemoved()) {
            properties.remove(removedPropertyName);
        }

        updatedDocumentWriter.setProperties(properties);
    }

    @Override
    public Document getChildren( PageKey pageKey ) {
        String parentId = pageKey.getParentId();
        Document doc = documentsById.get(parentId);
        assert doc != null;
        DocumentReader reader = readDocument(doc);
        List<? extends Document> children = reader.getChildren();

        int blockSize = (int)pageKey.getBlockSize();
        int offset = pageKey.getOffsetInt();

        DocumentWriter writer = newDocument(parentId).setChildren(children.subList(offset, offset + blockSize));
        if (offset + blockSize == children.size()) {
            return writer.document();
        }
        return writer.addPage(parentId, offset + 1, blockSize, children.size()).document();
    }
}
