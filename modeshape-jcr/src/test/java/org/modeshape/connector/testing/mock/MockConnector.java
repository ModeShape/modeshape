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
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.federation.Connector;
import org.modeshape.jcr.federation.Connector.DocumentReader;
import org.modeshape.jcr.federation.paging.BlockKey;
import org.modeshape.jcr.federation.paging.Pageable;
import org.modeshape.jcr.federation.paging.PagingWriter;

@Deprecated
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
                            NodeTypeManager nodeTypeManager ) /*throws RepositoryException, IOException*/{

        String id1 = newId();
        EditableDocument doc1 = newDocument(id1).addProperty(nameFrom("federated1_prop1"), "a string")
                                                .addProperty("federated1_prop2", 12)
                                                .addProperty(JcrLexicon.PRIMARY_TYPE, "nt:unstructured")
                                                .document();
        documentsByLocation.put("/doc1", doc1);
        documentsById.put(id1, doc1);

        String id2 = newId();
        String id3 = newId();
        EditableDocument doc3 = newDocument(id3).addProperty("federated3_prop1", "yet another string")
                                                .addProperty(JcrLexicon.PRIMARY_TYPE, "nt:unstructured")
                                                .setParent(id2)
                                                .document();
        documentsById.put(id3, doc3);
        documentsByLocation.put("/doc2/doc3", doc3);

        EditableDocument doc2 = newDocument(id2).addProperty("federated2_prop1", "another string")
                                                .addProperty("federated2_prop2", Boolean.FALSE)
                                                .addProperty(JcrLexicon.PRIMARY_TYPE, "nt:unstructured")
                                                .addChild(id3, "federated3")
                                                .document();
        documentsByLocation.put("/doc2", doc2);
        documentsById.put(id2, doc2);

        String id4 = newId();
        EditableDocument doc4 = newDocument(id4).addProperty(JcrLexicon.PRIMARY_TYPE, "nt:unstructured")
                                                .setParent(PAGED_DOCUMENT_ID)
                                                .document();
        documentsById.put(id4, doc4);

        String id5 = newId();
        EditableDocument doc5 = newDocument(id5).addProperty(JcrLexicon.PRIMARY_TYPE, "nt:unstructured")
                                                .setParent(PAGED_DOCUMENT_ID)
                                                .document();
        documentsById.put(id5, doc5);

        String id6 = newId();
        EditableDocument doc6 = newDocument(id6).addProperty(JcrLexicon.PRIMARY_TYPE, "nt:unstructured")
                                                .setParent(PAGED_DOCUMENT_ID)
                                                .document();
        documentsById.put(id6, doc6);

        EditableDocument pagedDoc = newDocument(PAGED_DOCUMENT_ID).addProperty(JcrLexicon.PRIMARY_TYPE, "nt:unstructured")
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
        if (PAGED_DOCUMENT_ID.equals(id)) {
            DocumentReader reader = readDocument(doc);
            List<EditableDocument> children = reader.getChildren();
            PagingWriter pagingWriter = newPagedDocument(doc);
            pagingWriter.newBlock(children.subList(0, 1), reader.getDocumentId(), String.valueOf(1), 1, children.size());
            return pagingWriter.document();
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
    public void updateDocument( String id,
                                Document document ) {
        if (!documentsById.containsKey(id)) {
            return;
        }
        Document existingDocument = documentsById.get(id);
        EditableDocument mergedDocument = newDocument(existingDocument).merge(document).document();
        documentsById.put(id, mergedDocument);
    }

    @Override
    public void shutdown() {
        // do nothing
    }

    @Override
    public Document getChildrenBlock( BlockKey blockKey ) {
        String parentId = blockKey.getParentId();
        Document doc = documentsById.get(parentId);
        assert doc != null;
        Connector.DocumentReader reader = readDocument(doc);
        List<EditableDocument> children = reader.getChildren();

        int blockSize = (int)blockKey.getBlockSize();
        int offset = Integer.valueOf(blockKey.getOffset());
        List<EditableDocument> childrenForBlock = children.subList(offset, offset + blockSize);

        if (offset + blockSize == children.size()) {
            return newPagedDocument().endBlock(childrenForBlock).document();
        }
        return newPagedDocument().newBlock(childrenForBlock, parentId, String.valueOf(offset + 1), blockSize, children.size())
                                 .document();
    }
}
