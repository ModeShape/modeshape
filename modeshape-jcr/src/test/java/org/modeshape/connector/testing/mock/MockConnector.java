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
package org.modeshape.connector.testing.mock;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

public class MockConnector extends Connector implements Pageable {
    public static final String SOURCE_NAME = "mock-source";
    public static final String PAGED_DOC_LOCATION = "/pagedDoc";

    private final String PAGED_DOCUMENT_ID = newId();

    private final Map<String, EditableDocument> documentsByLocation = new HashMap<String, EditableDocument>();
    private final Map<String, Document> documentsById = new HashMap<String, Document>();

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) {

        String id1 = newId();
        EditableDocument doc1 = newDocument(id1).setPrimaryType(JcrNtLexicon.UNSTRUCTURED)
                                                .addProperty(nameFrom("federated1_prop1"), "a string")
                                                .addProperty("federated1_prop2", 12)
                                                .document();
        documentsByLocation.put("/doc1", doc1);
        documentsById.put(id1, doc1);

        String id2 = newId();
        String id3 = newId();
        EditableDocument doc3 = newDocument(id3).setPrimaryType(JcrNtLexicon.UNSTRUCTURED)
                                                .addProperty("federated3_prop1", "yet another string")
                                                .setParent(id2)
                                                .document();
        documentsById.put(id3, doc3);
        documentsByLocation.put("/doc2/doc3", doc3);

        EditableDocument doc2 = newDocument(id2).setPrimaryType(JcrNtLexicon.UNSTRUCTURED)
                                                .addProperty("federated2_prop1", "another string")
                                                .addProperty("federated2_prop2", Boolean.FALSE)
                                                .addChild(id3, "federated3")
                                                .document();
        documentsByLocation.put("/doc2", doc2);
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
        EditableDocument document = documentsByLocation.get(path);
        return document != null ? readDocument(document).getDocumentId() : null;
    }

    @Override
    public boolean removeDocument( String id ) {
        Document doc = documentsById.remove(id);
        if (doc != null) {
            for (Iterator<Map.Entry<String, EditableDocument>> iterator = documentsByLocation.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<String, EditableDocument> entry = iterator.next();
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
        Document document = documentChanges.getDocument();
        if (!documentsById.containsKey(id)) {
            return;
        }

        DocumentReader changes = readDocument(document);
        DocumentWriter updatedDocument = newDocument(id).setChildren(changes.getChildren())
                                                        .setProperties(changes.getProperties());
        documentsById.put(id, updatedDocument.document());
    }

    @Override
    public void shutdown() {
        // do nothing
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
