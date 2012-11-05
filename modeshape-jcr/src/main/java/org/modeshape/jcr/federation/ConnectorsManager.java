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
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.cache.NodeKey;

/**
 * Class which maintains (based on the configuration) the list of available connectors for a repository.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * //TODO author=Horia Chiorean date=11/1/12 description=This should get the configuration from the running state and initialize the connectors
 */
public class ConnectorsManager {

    private final Map<String, Connector> sourceKeyToConnectorMap;

    public ConnectorsManager( Map<String, Connector> sourceKeyToConnectorMap ) {
        this.sourceKeyToConnectorMap = sourceKeyToConnectorMap;
    }

    public ConnectorsManager() {
        this.sourceKeyToConnectorMap = new HashMap<String, Connector>();
        this.sourceKeyToConnectorMap.put(MockConnector.SOURCE_KEY, new MockConnector());
    }

    public void addConnector(String sourceName, Connector connector) {
        sourceKeyToConnectorMap.put(NodeKey.keyForSourceName(sourceName), connector);
    }

    public Connector getConnectorForSourceName( String sourceName ) {
        assert sourceName != null;
        return sourceKeyToConnectorMap.get(NodeKey.keyForSourceName(sourceName));
    }

    public Connector getConnectorForSourceKey( String sourceKey ) {
        return sourceKeyToConnectorMap.get(sourceKey);
    }

    public boolean hasConnectors() {
        return !sourceKeyToConnectorMap.isEmpty();
    }


    //TODO author=Horia Chiorean date=11/1/12 description=Should be removed after POC
    public static class MockConnector implements Connector {
        public static final String SOURCE_NAME = "mock-source";
        public static final String SOURCE_KEY = NodeKey.keyForSourceName(SOURCE_NAME);

        private static final Map<String, EditableDocument> documentsByLocation;
        private static final Map<String, EditableDocument> documentsById;

        static  {
            documentsByLocation = new HashMap<String, EditableDocument>();
            documentsById = new HashMap<String, EditableDocument>();

            String id1 = newId();
            EditableDocument doc1 = new FederatedDocumentBuilder().createDocument(id1, "federated1")
                                                                  .addProperty("federated1_prop1", "a string")
                                                                  .addProperty("federated1_prop2", 12)
                                                                  .addProperty(JcrLexicon.PRIMARY_TYPE, "nt:unstructured")
                                                                  .build();
            documentsByLocation.put("/doc1", doc1);
            documentsById.put(id1, doc1);

            String id3 = newId();
            EditableDocument doc3 = new FederatedDocumentBuilder().createDocument(id3, "federated3")
                                                                  .addProperty("federated3_prop1", "yet another string")
                                                                  .addProperty(JcrLexicon.PRIMARY_TYPE, "nt:unstructured")
                                                                  .build();
            String id2 = newId();
            EditableDocument doc2 = new FederatedDocumentBuilder().createDocument(id2, "federated2")
                                                                  .addProperty("federated2_prop1", "another string")
                                                                  .addProperty("federated2_prop2", Boolean.FALSE)
                                                                  .addProperty(JcrLexicon.PRIMARY_TYPE, "nt:unstructured")
                                                                  .addChild(id3, "federated3")
                                                                  .build();



            documentsByLocation.put("/doc2", doc2);
            documentsByLocation.put("/doc2/doc3", doc3);
            documentsById.put(id2, doc2);
            documentsById.put(id3, doc3);
        }

        private static String newId() {
            return UUID.randomUUID().toString();
        }

        @Override
        public EditableDocument getDocumentById( String id ) {
            return documentsById.get(id);
        }

        @Override
        public String getSourceName() {
            return SOURCE_NAME;
        }

        @Override
        public EditableDocument getDocumentAtLocation( String location ) {
            return documentsByLocation.get(location);
        }

        @Override
        public void removeDocument( String id ) {
            EditableDocument doc = documentsById.remove(id);
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
            String id = newId();
            documentsById.put(id, new FederatedDocumentBuilder(document).build());
        }

        @Override
        public void updateDocument( String id,
                                    Document document ) {
            if (!documentsById.containsKey(id)) {
                return;
            }
            EditableDocument existingDocument = documentsById.get(id);
            EditableDocument mergedDocument = new FederatedDocumentBuilder(existingDocument).merge(document).build();
            documentsById.put(id, mergedDocument);
        }
    }
}
