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
package org.modeshape.connector.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.JcrNtLexicon;
import org.modeshape.jcr.spi.federation.ConnectorChangeSet;
import org.modeshape.jcr.spi.federation.DocumentReader;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * An extension of the {@link MockConnector} that will automatically create some content and generate events.
 * <p>
 * When a new node is created underneath '/doc{n}/generate' with some name (e.g., 'foo'), the connector automatically creates a
 * new node under '/doc{n}/generate-out' with the same name (e.g., 'foo') and with several child nodes. When a node under
 * '/doc{n}/generate' is removed, then the corresponding node under '/doc{n}/generate-out' will also be removed and events will be
 * produced.
 * </p>
 */
public class MockConnectorWithChanges extends MockConnector {

    @Override
    protected void createInitialNodes() {
        super.createInitialNodes();

        for (Map.Entry<String, Document> entry : new HashMap<String, Document>(documentsByLocation).entrySet()) { // copy
            String pathStr = entry.getKey();
            Path path = pathFrom(pathStr);
            if (path.size() == 1 && isAutoCreatedNode(path)) {
                String parentId = readDocument(entry.getValue()).getDocumentId();

                String id1 = newId();
                Document doc1 = newDocument(id1).setPrimaryType("nt:unstructured").setParent(parentId).document();
                documentsById.put(id1, doc1);
                documentsByLocation.put(pathStr + "/generate", doc1);

                String id2 = newId();
                Document doc2 = newDocument(id1).setPrimaryType("nt:unstructured").setParent(parentId).document();
                documentsById.put(id2, doc2);
                documentsByLocation.put(pathStr + "/generated-out", doc2);

                DocumentWriter parentWriter = writeDocument(entry.getValue());
                parentWriter.addChild(id1, "generate");
                parentWriter.addChild(id2, "generated-out");
                Document parent = parentWriter.document();
                documentsById.put(readDocument(parent).getDocumentId(), parent);
                documentsByLocation.put(pathStr, parent);
            }
        }
    }

    protected boolean isAutoCreatedNode( Path path ) {
        return path.getLastSegment().getName().getLocalName().startsWith("doc");
    }

    protected String pathFromDocumentId( String documentId ) {
        Collection<String> paths = super.getDocumentPathsById(documentId);
        assert !paths.isEmpty();
        return paths.iterator().next();
    }

    @Override
    protected void storedDocument( String documentId,
                                   Document document ) {
        String docPath = pathFromDocumentId(documentId);
        assert docPath != null;
        Path path = pathFrom(docPath);
        if (path.size() == 3 && path.getSegment(1).getName().getLocalName().equals("generate")) {
            List<DocInfo> newDocs = new ArrayList<DocInfo>();
            ConnectorChangeSet changes = newConnectorChangedSet();
            Name topName = path.getSegment(0).getName();
            Name name = path.getSegment(2).getName();

            // Find the parent ...
            String generatedOutPath = "/" + topName.getLocalName() + "/generated-out";
            String generatedOutId = getDocumentId(generatedOutPath);
            Document generatedOutDoc = getDocumentById(generatedOutId);
            DocumentWriter generatedOutWriter = writeDocument(generatedOutDoc);

            // Create a document at '{topName}/generated-out/{name}'
            String newId = newId();
            String newPath = generatedOutPath + "/" + stringFrom(name);
            DocumentWriter writer = newDocument(newId);
            writer.setPrimaryType("nt:unstructured");
            writer.setParent(generatedOutId);
            writer.addProperty("prop1", "value1");
            writer.addProperty("prop2", "value2");
            newDocs.add(new DocInfo(writer.document(), newId, newPath));
            DocumentReader reader = readDocument(writer.document());
            changes.nodeCreated(newId, documentId, newPath, JcrNtLexicon.UNSTRUCTURED, Collections.<Name>emptySet(),
                                reader.getProperties());

            // And some children ...
            for (int i = 0; i != 3; ++i) {
                String childName = "child" + i;
                String childId = newId();
                String childPath = newPath + "/" + childName;
                DocumentWriter childWriter = newDocument(childId);
                childWriter.setPrimaryType("nt:unstructured");
                childWriter.setParent(newId);
                childWriter.addProperty("prop1", "value1");
                childWriter.addProperty("prop2", "value2");
                childWriter.setParent(newId);
                writer.addChild(childId, childName);
                newDocs.add(new DocInfo(childWriter.document(), childId, childPath));
                DocumentReader childReader = readDocument(writer.document());
                changes.nodeCreated(childId, newId, childPath, JcrNtLexicon.UNSTRUCTURED, Collections.<Name>emptySet(),
                                    childReader.getProperties());
            }

            for (DocInfo info : newDocs) {
                documentsById.put(info.id, info.doc);
                documentsByLocation.put(info.location, info.doc);
            }

            // Update the parent with a child reference to our new document ...
            generatedOutWriter.addChild(newId, name);
            Document doc = generatedOutWriter.document();
            documentsById.put(generatedOutId, doc);
            documentsByLocation.put(generatedOutPath, doc);

            // Now generate events ...
            changes.publish(null);
        }
    }

    protected static class DocInfo {
        protected Document doc;
        protected String id;
        protected String location;

        protected DocInfo( Document doc,
                           String id,
                           String location ) {
            this.doc = doc;
            this.id = id;
            this.location = location;
        }
    }

    @Override
    protected void preRemoveDocument( String documentId,
                                      Document document ) {
        String docPath = pathFromDocumentId(documentId);
        Path path = pathFrom(docPath);
        if (path.size() == 3 && path.getSegment(1).getName().getLocalName().equals("generate")) {
            ConnectorChangeSet changes = newConnectorChangedSet();

            // Determine the id of the node we'll remove ...
            Name topName = path.getSegment(0).getName();
            Name name = path.getSegment(2).getName();
            String generatedOutPath = "/" + topName.getLocalName() + "/generated-out";
            Document generatedOutDoc = documentsByLocation.get(generatedOutPath);
            String generatedOutId = readDocument(generatedOutDoc).getDocumentId();

            String oldPath = generatedOutPath + "/" + name.getLocalName();
            Document oldDoc = documentsByLocation.get(oldPath);
            String oldId = readDocument(oldDoc).getDocumentId();

            // Remove the child reference from '/doc{n}/generate-out' to the node we'll remove ...
            DocumentWriter generatedOutWriter = writeDocument(generatedOutDoc);
            generatedOutWriter.removeChild(oldId);
            persistDocument(generatedOutId, generatedOutWriter.document());

            // Remove the document at '/doc{n}/generate-out/{name}' ...
            removeDocument(oldId);
            changes.nodeRemoved(oldId, documentId, oldPath, JcrNtLexicon.UNSTRUCTURED, Collections.<Name>emptySet(),
                                JcrNtLexicon.UNSTRUCTURED, Collections.<Name>emptySet());

            // Remove the child documents, but we don't need to fire events for the subnodes of a deleted node ...
            DocumentReader reader = readDocument(oldDoc);
            for (String childId : reader.getChildrenMap().keySet()) {
                removeDocument(childId);
            }
            changes.publish(null);
        }
    }
}
