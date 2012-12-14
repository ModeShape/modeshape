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
import java.util.Arrays;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.api.Workspace;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.federation.spi.ConnectorException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Unit test for the {@link org.modeshape.connector.mock.MockConnector} which validates several areas around the connector SPI
 * and federation design.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class MockConnectorTest extends SingleUseAbstractTest {

    private FederationManager federationManager;
    private Node testRoot;

    @Before
    public void before() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader().getResourceAsStream("config/repo-config-mock-federation.json"));

        testRoot = ((Node) session.getRootNode()).addNode("testRoot");
        testRoot.addNode("node1");
        session.save();

        federationManager = ((Workspace) session.getWorkspace()).getFederationManager();
    }

    @Test
    public void shouldCreateProjectionWithAlias() throws Exception {
        // link the first external document
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        assertEquals(2, testRoot.getNodes().getSize());

        Node doc1Federated = assertNodeFound("/testRoot/federated1");
        assertEquals(testRoot.getIdentifier(), doc1Federated.getParent().getIdentifier());
        assertEquals("a string", doc1Federated.getProperty("federated1_prop1").getString());
        assertEquals(12, doc1Federated.getProperty("federated1_prop2").getLong());

        // link a second external document with a sub-child
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC2_LOCATION, "federated2");
        assertEquals(3, testRoot.getNodes().getSize());

        Node doc2Federated = assertNodeFound("/testRoot/federated2");
        assertEquals(testRoot.getIdentifier(), doc2Federated.getParent().getIdentifier());
        assertEquals("another string", doc2Federated.getProperty("federated2_prop1").getString());
        assertEquals(false, doc2Federated.getProperty("federated2_prop2").getBoolean());

        Node doc2FederatedChild = assertNodeFound("/testRoot/federated2/federated3");
        assertEquals("yet another string", doc2FederatedChild.getProperty("federated3_prop1").getString());
    }

    @Test
    public void shouldCreateProjectionWithoutAlias() throws Exception {
        // link the first external document
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, null);
        assertEquals(2, testRoot.getNodes().getSize());

        Node doc1Federated = assertNodeFound("/testRoot" + MockConnector.DOC1_LOCATION);
        assertEquals(testRoot.getIdentifier(), doc1Federated.getParent().getIdentifier());
        assertEquals("a string", doc1Federated.getProperty("federated1_prop1").getString());
        assertEquals(12, doc1Federated.getProperty("federated1_prop2").getLong());
    }

    @Test
    public void shouldCreateExternalNode() throws Exception {
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        Node externalNode1 = doc1Federated.addNode("federated1_1", null);
        externalNode1.setProperty("prop1", "a value");
        externalNode1.addNode("federated1_1_1", null);

        session.save();

        Node federated1_1 = doc1Federated.getNode("federated1_1");
        assertNotNull(federated1_1);
        assertEquals(doc1Federated, federated1_1.getParent());
        assertEquals(1, doc1Federated.getNodes().getSize());
        assertNotNull(session.getNode("/testRoot/federated1/federated1_1"));
        assertEquals("a value", federated1_1.getProperty("prop1").getString());

        Node federated1_1_1 = assertNodeFound("/testRoot/federated1/federated1_1/federated1_1_1");
        assertEquals(federated1_1, federated1_1_1.getParent());
    }

    @Test
    public void shouldUpdateExternalNodeProperties() throws Exception {
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        Node externalNode1 = doc1Federated.addNode("federated1_1", null);
        externalNode1.setProperty("prop1", "a value");
        externalNode1.setProperty("prop2", "a value 2");
        session.save();

        externalNode1.setProperty("prop1", "edited value");
        assertEquals("a value 2", externalNode1.getProperty("prop2").getString());
        externalNode1.getProperty("prop2").remove();
        externalNode1.setProperty("prop3", "a value 3");
        session.save();

        Node federated1_1 = doc1Federated.getNode("federated1_1");
        assertEquals("edited value", federated1_1.getProperty("prop1").getString());
        assertEquals("a value 3", federated1_1.getProperty("prop3").getString());
        try {
            federated1_1.getProperty("prop2");
            fail("Property was not removed from external node");
        } catch (PathNotFoundException e) {
            //expected
        }
    }

    @Test
    public void shouldUpdateExternalNodeMixins() throws Exception {
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        Node externalNode1 = doc1Federated.addNode("federated1_1", null);

        externalNode1.addMixin("mix:created");
        session.save();

        externalNode1 = session.getNode("/testRoot/federated1/federated1_1");
        NodeType[] mixins = externalNode1.getMixinNodeTypes();
        assertEquals(1, mixins.length);
        assertEquals("mix:created", mixins[0].getName());
        externalNode1.removeMixin("mix:created");
        externalNode1.addMixin("mix:lastModified");
        session.save();

        externalNode1 = session.getNode("/testRoot/federated1/federated1_1");
        mixins = externalNode1.getMixinNodeTypes();
        assertEquals(1, mixins.length);
        assertEquals("mix:lastModified", mixins[0].getName());
    }

    @Test
    public void shouldUpdateExternalNodeChildren() throws Exception {
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        doc1Federated.addNode("federated1_1", null);
        session.save();

        String externalNodePath = "/testRoot/federated1/federated1_1";
        assertExternalNodeHasChildren(externalNodePath);

        Node externalNode = session.getNode(externalNodePath);
        externalNode.addNode("child1");
        externalNode.addNode("child2");
        session.save();

        assertExternalNodeHasChildren(externalNodePath, "child1", "child2");

        externalNode = session.getNode(externalNodePath);
        externalNode.getNode("child1").remove();
        externalNode.getNode("child2").remove();
        externalNode.addNode("child3");
        session.save();

        assertExternalNodeHasChildren(externalNodePath, "child3");
    }

    @Test
    public void shouldMoveExternalNode() throws Exception {
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        Node parent1 = doc1Federated.addNode("parent1", null);
        parent1.addNode("child1");
        parent1.addNode("childX");
        parent1.addNode("child2");

        Node parent2 = doc1Federated.addNode("parent2", null);
        parent2.addNode("child3");
        parent2.addNode("child4");

        session.save();
        assertExternalNodeHasChildren("/testRoot/federated1/parent1", "child1", "childX", "child2");
        assertExternalNodeHasChildren("/testRoot/federated1/parent2", "child3", "child4");

        ((Workspace) session.getWorkspace()).move("/testRoot/federated1/parent1/childX", "/testRoot/federated1/parent2/childX");

        assertExternalNodeHasChildren("/testRoot/federated1/parent1", "child1", "child2");
        assertExternalNodeHasChildren("/testRoot/federated1/parent2", "child3", "child4", "childX");
    }

    @Test
    public void shouldReorderExternalNodes() throws Exception{
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        Node parent1 = doc1Federated.addNode("parent1", null);
        parent1.addNode("child1");
        parent1.addNode("child2");
        parent1.addNode("child3");
        session.save();

        assertExternalNodeHasChildren("/testRoot/federated1/parent1", "child1", "child2", "child3");
        parent1.orderBefore("child1", "child2");
        session.save();
        assertExternalNodeHasChildren("/testRoot/federated1/parent1", "child2", "child1", "child3");
    }

    @Test
    public void shouldNotAllowInternalNodesAsReferrers() throws Exception {
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        Node externalNode = doc1Federated.addNode("federated1_1", null);
        externalNode.addMixin("mix:referenceable");
        session.save();

        Value weakRef = session.getValueFactory().createValue(externalNode, true);
        testRoot.setProperty("weakRef", weakRef);
        try {
            session.save();
            fail("It should not be possible to create weak references from internal nodes to external nodes");
        } catch (RepositoryException e) {
            assertTrue(e.getCause() instanceof ConnectorException);
        }

        Value strongRef = session.getValueFactory().createValue(externalNode, false);
        testRoot.setProperty("strongRef", strongRef);
        try {
            session.save();
            fail("It should not be possible to create strong references from internal nodes to external nodes");
        } catch (RepositoryException e) {
            assertTrue(e.getCause() instanceof ConnectorException);
        }
    }

    @Test
    public void shouldRemoveExternalNode() throws Exception {
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        doc1Federated.addNode("federated1_1", null);
        session.save();

        Node externalNode = session.getNode("/testRoot/federated1/federated1_1");
        externalNode.remove();
        session.save();

        assertNodeNotFound("/testRoot/federated1/federated1_1");
    }

    @Test
    public void shouldRemoveProjectionViaNodeRemove() throws Exception {
        testRoot.addNode("child1");
        session.save();

        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC2_LOCATION, "federated2");

        Node projection = session.getNode("/testRoot/federated1");
        projection.remove();
        session.save();
        assertNodeNotFound("/testRoot/federated1");
        assertNodeFound("/testRoot/federated2");
        assertNodeFound("/testRoot/child1");

        projection = session.getNode("/testRoot/federated2");
        projection.remove();
        session.save();

        assertNodeNotFound("/testRoot/federated2");
        assertNodeFound("/testRoot/child1");
    }

    @Test
    public void removingProjectionViaNodeRemoveShouldDeleteExternalNodes() throws Exception {
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC2_LOCATION, "projection1");
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC2_LOCATION, "projection2");

        Node projection1 = assertNodeFound("/testRoot/projection1/federated3");
        assertNodeFound("/testRoot/projection2/federated3");

        projection1.remove();
        session.save();
        assertNodeNotFound("/testRoot/projection2/federated3");
    }

    @Test
    public void removeProjectionViaFederationManagerShouldNotDeleteExternalNode() throws Exception {
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC2_LOCATION, "projection1");
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC2_LOCATION, "projection2");

        federationManager.removeProjection("/testRoot/projection1");
        assertNodeFound("/testRoot/projection2/federated3");
    }

    @Test
    public void shouldRemoveProjectionViaFederationManager() throws Exception {
        testRoot.addNode("child1");
        session.save();

        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC2_LOCATION, "federated2");

        federationManager.removeProjection("/testRoot/federated2");
        assertNodeFound("/testRoot/federated1");
        assertNodeFound("/testRoot/child1");
        assertNodeNotFound("/testRoot/federated2");

        federationManager.removeProjection("/testRoot/federated1");
        assertNodeNotFound("/testRoot/federation1");
        assertNodeFound("/testRoot/child1");
    }

    @Test
    public void removingInternalNodeShouldNotRemoveExternalNodes() throws Exception {
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC2_LOCATION,"federated2");

        Node internalNode1 = testRoot.addNode("internalNode1");
        session.save();
        federationManager.createProjection("/testRoot/internalNode1", MockConnector.SOURCE_NAME,
                                           MockConnector.DOC2_LOCATION, "federated2");

        //remove the federated node directly
        assertNodeFound("/testRoot/internalNode1/federated2/federated3");
        internalNode1.remove();
        session.save();
        //check external nodes are still there
        assertNodeFound("/testRoot/federated2/federated3");

        testRoot.addNode("internalNode2").addNode("internalNode2_1");
        session.save();
        federationManager.createProjection("/testRoot/internalNode2/internalNode2_1", MockConnector.SOURCE_NAME,
                                           MockConnector.DOC2_LOCATION,
                                           "federated2");
        //remove an ancestor of the federated node
        assertNodeFound("/testRoot/internalNode2/internalNode2_1/federated2/federated3");
        ((Node) session.getNode("/testRoot/internalNode2")).remove();
        session.save();

        //check external nodes are still there
        assertNodeFound("/testRoot/federated2/federated3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotRemoveProjectionUsingRootPath() throws Exception {
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        federationManager.removeProjection("/");
    }

    @Test(expected = PathNotFoundException.class)
    public void shouldNotRemoveProjectionIfPathInvalid() throws Exception {
        federationManager.createProjection("/testRoot", MockConnector.SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        federationManager.removeProjection("/testRoot/federated");
    }

    @Test
    public void shouldNavigateChildrenFromPagedConnector() throws Exception {
        federationManager.createProjection("/testRoot",
                                           MockConnector.SOURCE_NAME,
                                           MockConnector.PAGED_DOC_LOCATION,
                                           "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        NodeIterator nodesIterator = doc1Federated.getNodes();
        assertEquals(3, nodesIterator.getSize());

        List<String> childrenNames = new ArrayList<String>(3);
        while (nodesIterator.hasNext()) {
            childrenNames.add(nodesIterator.nextNode().getName());
        }
        assertEquals(Arrays.asList("federated4", "federated5", "federated6"), childrenNames);
    }

    private void assertExternalNodeHasChildren( String externalNodePath,
                                                String... children ) throws Exception {
        Node externalNode = session.getNode(externalNodePath);
        NodeIterator childNodes = externalNode.getNodes();

        if (children.length == 0) {
            assertEquals(0, childNodes.getSize());
            return;
        }
        List<String> actualNodes = new ArrayList<String>((int) childNodes.getSize());
        while (childNodes.hasNext()) {
            actualNodes.add(childNodes.nextNode().getName());
        }

        assertEquals(Arrays.asList(children), actualNodes);
    }

    private void assertNodeNotFound(String absPath) throws RepositoryException {
        try {
            session.getNode(absPath);
            fail("Node at " + absPath + " should not exist");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    private Node assertNodeFound( String absPath ) throws RepositoryException {
        Node node = session.getNode(absPath);
        assertNotNull(node);
        return node;
    }

}
