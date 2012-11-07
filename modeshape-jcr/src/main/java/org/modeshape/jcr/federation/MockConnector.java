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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.jcr.NamespaceRegistry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

@Deprecated
public class MockConnector extends Connector {
    public static final String SOURCE_NAME = "mock-source";

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

        String id3 = newId();
        EditableDocument doc3 = newDocument(id3).addProperty("federated3_prop1", "yet another string")
                                                              .addProperty(JcrLexicon.PRIMARY_TYPE, "nt:unstructured")
                                                              .document();
        String id2 = newId();
        EditableDocument doc2 = newDocument(id2).addProperty("federated2_prop1", "another string")
                                                              .addProperty("federated2_prop2", Boolean.FALSE)
                                                              .addProperty(JcrLexicon.PRIMARY_TYPE, "nt:unstructured")
                                                              .addChild(id3, "federated3")
                                                              .document();

        documentsByLocation.put("/doc2", doc2);
        documentsByLocation.put("/doc2/doc3", doc3);
        documentsById.put(id2, doc2);
        documentsById.put(id3, doc3);
    }

    @Override
    public Document getDocumentById( String id ) {
        return documentsById.get(id);
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
    public void removeDocument( String id ) {
        Document doc = documentsById.remove(id);
        if (doc != null) {
            for (Iterator<Map.Entry<String, EditableDocument>> iterator = documentsByLocation.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<String, EditableDocument> entry = iterator.next();
                if (entry.getValue().equals(doc)) {
                    iterator.remove();
                    return;
                }
            }
        }
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
}
