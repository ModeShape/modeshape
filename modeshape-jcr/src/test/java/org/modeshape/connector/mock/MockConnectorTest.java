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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.modeshape.jcr.ValidateQuery.validateQuery;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.ModeShapePermissions;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.api.Workspace;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.api.query.QueryManager;
import org.modeshape.jcr.spi.federation.ConnectorException;

/**
 * Unit test for the {@link org.modeshape.connector.mock.MockConnector} which validates several areas around the connector SPI and
 * federation design.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class MockConnectorTest extends SingleUseAbstractTest {
    private static final String SOURCE_NAME = "mock-source";

    private FederationManager federationManager;
    private Node testRoot;

    @Before
    @Override
    public void beforeEach() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader()
                                                   .getResourceAsStream("config/repo-config-mock-federation.json"));

        testRoot = ((Node)session.getRootNode()).addNode("testRoot");
        session.save();

        federationManager = ((Workspace)session.getWorkspace()).getFederationManager();
    }

    @Test
    public void shouldCreateProjectionWithAlias() throws Exception {
        // add an internal node
        testRoot.addNode("node1");
        session.save();

        // link the first external document
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        assertEquals(2, testRoot.getNodes().getSize());

        Node doc1Federated = assertNodeFound("/testRoot/federated1");
        assertEquals(testRoot.getIdentifier(), doc1Federated.getParent().getIdentifier());
        assertEquals("a string", doc1Federated.getProperty("federated1_prop1").getString());
        assertEquals(12, doc1Federated.getProperty("federated1_prop2").getLong());

        // link a second external document with a sub-child
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "federated2");
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
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, null);
        assertEquals(1, testRoot.getNodes().getSize());

        Node doc1Federated = assertNodeFound("/testRoot" + MockConnector.DOC1_LOCATION);
        assertEquals(testRoot.getIdentifier(), doc1Federated.getParent().getIdentifier());
        assertEquals("a string", doc1Federated.getProperty("federated1_prop1").getString());
        assertEquals(12, doc1Federated.getProperty("federated1_prop2").getLong());
    }

    @Test
    public void shouldCreateExternalNode() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
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
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
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
            // expected
        }
    }

    @Test
    public void shouldUpdateExternalNodeMixins() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
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
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
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
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
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

        ((Workspace)session.getWorkspace()).move("/testRoot/federated1/parent1/childX", "/testRoot/federated1/parent2/childX");

        assertExternalNodeHasChildren("/testRoot/federated1/parent1", "child1", "child2");
        assertExternalNodeHasChildren("/testRoot/federated1/parent2", "child3", "child4", "childX");
    }

    @Test
    public void shouldReorderExternalNodes() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
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
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
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
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
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

        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "federated2");

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
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "projection1");
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "projection2");

        Node projection1 = assertNodeFound("/testRoot/projection1/federated3");
        assertNodeFound("/testRoot/projection2/federated3");

        projection1.remove();
        session.save();
        assertNodeNotFound("/testRoot/projection2/federated3");
    }

    @Test
    public void removeProjectionViaFederationManagerShouldNotDeleteExternalNode() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "projection1");
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "projection2");

        federationManager.removeProjection("/testRoot/projection1");
        assertNodeFound("/testRoot/projection2/federated3");
    }

    @Test
    public void shouldRemoveProjectionViaFederationManager() throws Exception {
        testRoot.addNode("child1");
        session.save();

        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "federated2");

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
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "federated2");

        Node internalNode1 = testRoot.addNode("internalNode1");
        session.save();
        federationManager.createProjection("/testRoot/internalNode1", SOURCE_NAME, MockConnector.DOC2_LOCATION, "federated2");

        // remove the federated node directly
        assertNodeFound("/testRoot/internalNode1/federated2/federated3");
        internalNode1.remove();
        session.save();
        // check external nodes are still there
        assertNodeFound("/testRoot/federated2/federated3");

        testRoot.addNode("internalNode2").addNode("internalNode2_1");
        session.save();
        federationManager.createProjection("/testRoot/internalNode2/internalNode2_1", SOURCE_NAME, MockConnector.DOC2_LOCATION,
                                           "federated2");
        // remove an ancestor of the federated node
        assertNodeFound("/testRoot/internalNode2/internalNode2_1/federated2/federated3");
        ((Node)session.getNode("/testRoot/internalNode2")).remove();
        session.save();

        // check external nodes are still there
        assertNodeFound("/testRoot/federated2/federated3");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotRemoveProjectionUsingRootPath() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        federationManager.removeProjection("/");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotRemoveProjectionIfPathInvalid() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        federationManager.removeProjection("/testRoot/federated");
    }

    @Test
    public void shouldNavigateChildrenFromPagedConnector() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.PAGED_DOC_LOCATION, "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        NodeIterator nodesIterator = doc1Federated.getNodes();
        assertEquals(3, nodesIterator.getSize());

        List<String> childrenNames = new ArrayList<String>(3);
        while (nodesIterator.hasNext()) {
            childrenNames.add(nodesIterator.nextNode().getName());
        }
        assertEquals(Arrays.asList("federated4", "federated5", "federated6"), childrenNames);
    }

    @Test
    public void shouldIndexProjectionsAndExternalNodes() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "federated2");

        Workspace workspace = session.getWorkspace();
        workspace.reindex();

        QueryManager queryManager = workspace.getQueryManager();
        Query query = queryManager.createQuery("select * FROM [nt:base] WHERE [jcr:path] LIKE '/testRoot/federated1'",
                                               Query.JCR_SQL2);
        assertEquals(1, query.execute().getNodes().getSize());

        query = queryManager.createQuery("select * FROM [nt:base] WHERE [jcr:path] LIKE '/testRoot/federated2'", Query.JCR_SQL2);
        assertEquals(1, query.execute().getNodes().getSize());

        Node externalNode = session.getNode("/testRoot/federated2/federated3");
        externalNode.setProperty("test", "a value");
        session.save();

        query = queryManager.createQuery("select * FROM [nt:base] as a WHERE a.test='a value'", Query.JCR_SQL2);
        assertEquals(1, query.execute().getNodes().getSize());

        query = queryManager.createQuery("select * FROM [nt:base] WHERE [jcr:path] LIKE '/testRoot/federated2/federated3'",
                                         Query.JCR_SQL2);
        assertEquals(1, query.execute().getNodes().getSize());
    }

    @Test
    public void shouldNotIndexNotQueryableConnector() throws Exception {
        federationManager.createProjection("/testRoot", "mock-source-non-queryable", MockConnector.DOC2_LOCATION, "federated2");

        Workspace workspace = session.getWorkspace();
        workspace.reindex();

        QueryManager queryManager = workspace.getQueryManager();
        Query query = queryManager.createQuery("select * FROM [nt:base] WHERE [jcr:path] LIKE '/testRoot/federated2'",
                                               Query.JCR_SQL2);
        assertEquals(0, query.execute().getNodes().getSize());

        query = queryManager.createQuery("select * FROM [nt:base] WHERE [jcr:path] LIKE '/testRoot/federated2/federated3'",
                                         Query.JCR_SQL2);
        assertEquals(0, query.execute().getNodes().getSize());

        Node externalNode = session.getNode("/testRoot/federated2/federated3");
        externalNode.setProperty("test", "a value");
        session.save();

        query = queryManager.createQuery("select * FROM [nt:base] as a WHERE a.test='a value'", Query.JCR_SQL2);
        assertEquals(0, query.execute().getNodes().getSize());
    }

    @Test
    public void shouldNotIndexNotQueryableDocument() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.NONT_QUERYABLE_DOC_LOCATION, "nonQueryableDoc");

        Workspace workspace = session.getWorkspace();
        workspace.reindex();

        Node externalNode = assertNodeFound("/testRoot/nonQueryableDoc");

        QueryManager queryManager = workspace.getQueryManager();
        Query query = queryManager.createQuery("select * FROM [nt:base] WHERE [jcr:path] LIKE '/testRoot/nonQueryableDoc'",
                                               Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        // change the document and re-run the query
        externalNode.setProperty("test", "a value");
        session.save();
        validateQuery().rowCount(0).validate(query, query.execute());
    }

    @Test( expected = RepositoryException.class )
    public void shouldNotAllowWritesIfReadonly() throws Exception {
        federationManager.createProjection("/testRoot", "mock-source-readonly", MockConnector.DOC1_LOCATION, "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        Node externalNode1 = doc1Federated.addNode("federated1_1", null);
        externalNode1.addNode("federated1_1_1", null);

        session.save();
    }

    @Test
    @FixFor( "MODE-1964" )
    public void shouldSendRemovedPropertiesToConnector() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "federated1");
        Node doc1Federated = session.getNode("/testRoot/federated1");
        Property doc1FederatedProperty = doc1Federated.getProperty("federated1_prop2");
        doc1FederatedProperty.remove();
        session.save();

        try {
            ((Node)session.getNode("/testRoot/federated1")).getProperty("federated1_prop2");
            fail("Property was not removed by connector");
        } catch (PathNotFoundException e) {
            // exception
        }
    }

    @Test
    @FixFor( "MODE-1977" )
    public void shouldNotAllowMoveIfSourceIsFederatedAndTargetIsNot() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        ((Node)session.getRootNode()).addNode("testRoot2");
        session.save();
        try {
            session.move("/testRoot", "/testRoot2");
            fail("Should not allow move is source is federated is target is not.");
        } catch (RepositoryException e) {
            // expected
            if (print) {
                e.printStackTrace();
            }
        }
        try {
            session.move("/testRoot/fed1", "/testRoot2");
            fail("Should not allow move is source is federated is target is not.");
        } catch (RepositoryException e) {
            // expected
            if (print) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @FixFor( "MODE-1977" )
    public void shouldNotAllowMoveIfSourceIsNotFederatedAndTargetIs() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        ((Node)session.getRootNode()).addNode("testRoot2");
        session.save();
        try {
            session.move("/testRoot2", "/testRoot/fed1");
            fail("Should not allow move is source is not federated and target is");
        } catch (RepositoryException e) {
            // expected
            if (print) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @FixFor( "MODE-1977" )
    public void shouldAllowMoveIfSourceIsNotFederatedAndTargetIsNotFederated() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        ((Node)session.getRootNode()).addNode("testRoot2").addNode("a");
        session.save();
        session.move("/testRoot2", "/testRoot");
        session.save();
        assertNodeFound("/testRoot[1]/fed1");
        assertNodeFound("/testRoot[2]/a");
    }

    @Test
    @FixFor( "MODE-1977" )
    public void shouldNotAllowMoveIfSourceAndTargetBelongToDifferentSources() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        federationManager.createProjection("/testRoot", "mock-source-non-queryable", MockConnector.DOC2_LOCATION, "fed2");
        try {
            session.move("/testRoot/fed1", "/testRoot/fed2");
            fail("Should not allow move if source and target don't belong to the same source");
        } catch (RepositoryException e) {
            // expected
            if (print) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @FixFor( "MODE-1977" )
    public void shouldNotAllowMoveIfSourceOrTargetIsProjection() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "fed2");
        try {
            session.move("/testRoot/fed2/federated3", "/testRoot/fed1");
            fail("Should not allow move if target is projection");
        } catch (RepositoryException e) {
            // expected
            if (print) {
                e.printStackTrace();
            }
        }

        try {
            session.move("/testRoot/fed2", "/testRoot/fed1");
            fail("Should not allow move if source is projection");
        } catch (RepositoryException e) {
            // expected
            if (print) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @FixFor( "MODE-1977" )
    public void shouldAllowMoveWithSameSource() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        ((Node)session.getNode("/testRoot/fed1")).addNode("federated1");
        session.save();
        assertNodeFound("/testRoot/fed1/federated1");

        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "fed2");
        assertNodeFound("/testRoot/fed2/federated3");

        session.move("/testRoot/fed1/federated1", "/testRoot/fed2/federated3");
        session.save();

        assertNodeFound("/testRoot/fed2/federated3[1]");
        assertNodeFound("/testRoot/fed2/federated3[2]");
        assertNodeNotFound("/testRoot/fed1/federated1");
    }

    @Test
    @FixFor( "MODE-1976" )
    public void shouldCopyFromFederatedSourceToNonFederatedTargetSameWs() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        Node federated1 = jcrSession().getNode("/testRoot/fed1").addNode("federated1");
        federated1.setProperty("prop", "value");
        jcrSession().getRootNode().addNode("testRoot2");
        jcrSession().save();

        jcrSession().getWorkspace().copy("/testRoot/fed1", "/testRoot2/fed1");
        assertNodeFound("/testRoot2/fed1");
        Node federated1Copy = assertNodeFound("/testRoot2/fed1/federated1");
        federated1Copy.remove();
        jcrSession().save();

        assertNodeFound("/testRoot/fed1/federated1");

        jcrSession().getRootNode().addNode("testRoot3");
        jcrSession().save();
        jcrSession().getWorkspace().copy("/testRoot/fed1/federated1", "/testRoot3");

        federated1Copy = assertNodeFound("/testRoot3[2]");
        assertNotNull(federated1Copy.getProperty("prop"));
    }

    @Test
    @FixFor( "MODE-1976" )
    public void shouldCopyFromNonFederatedSourceToFederatedTargetSameWs() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        jcrSession().getNode("/testRoot/fed1").addNode("federated1");
        jcrSession().getRootNode().addNode("testRoot2").addNode("nonFederated2");
        jcrSession().save();

        jcrSession().getWorkspace().copy("/testRoot2", "/testRoot/fed1/federated2");

        assertNodeFound("/testRoot/fed1/federated2");
        assertNodeFound("/testRoot/fed1/federated2/nonFederated2");
        assertEquals(2, jcrSession().getNode("/testRoot/fed1").getNodes().getSize());
    }

    @Test
    @FixFor( "MODE-1976" )
    public void shouldNotCopyIfSourceAndTargetSourcesDoNotMatch() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        federationManager.createProjection("/testRoot", "mock-source-non-queryable", MockConnector.DOC2_LOCATION, "fed2");
        try {
            jcrSession().getWorkspace().copy("/testRoot/fed1", "/testRoot/fed2/fed1");
            fail("Should not allow copy if source and target don't belong to the same source");
        } catch (RepositoryException e) {
            // expected
            if (print) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @FixFor( "MODE-1976" )
    public void shouldNotCopyIfSourceSubgraphContainsExternalNodesWhichDoNotMatchTargetSource() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        federationManager.createProjection("/testRoot", "mock-source-non-queryable", MockConnector.DOC1_LOCATION, "fed_nq1");
        federationManager.createProjection("/testRoot", "mock-source-non-queryable", MockConnector.DOC2_LOCATION, "fed2");
        try {
            jcrSession().getWorkspace().copy("/testRoot", "/testRoot/fed2/fed_mixed");
            fail("Should not allow copy if source subgraph contains nodes which don't belong to the same source as the target");
        } catch (RepositoryException e) {
            // expected
            if (print) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @FixFor( "MODE-1976" )
    public void shouldAllowCopyWithinSameSource() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "fed2");

        jcrSession().getWorkspace().copy("/testRoot/fed1", "/testRoot/fed2/fed1");
        assertNodeFound("/testRoot/fed2/fed1");
        assertNodeFound("/testRoot/fed2/federated3");
    }

    @Test
    @FixFor( "MODE-1975" )
    public void shouldNotAllowCloneWithinTheSameWs() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        try {
            jcrSession().getWorkspace().clone(jcrSession().getWorkspace().getName(), "/testRoot", "/testRoot1", false);
            fail("Should not be able to clone in the same ws if external nodes are involved");
        } catch (RepositoryException e) {
            // expected
            if (print) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @FixFor( "MODE-1975" )
    public void shouldNotAllowMoveWithinTheSameWsViaClone() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        try {
            // clone with removeExisting = true in the same ws is a move
            jcrSession().getWorkspace().clone(jcrSession().getWorkspace().getName(), "/testRoot", "/testRoot1", true);
            fail("Should not be able to clone in the same ws if external nodes are involved");
        } catch (RepositoryException e) {
            // expected
            if (print) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @FixFor( "MODE-1975" )
    public void shouldAllowCloneOnlyIfEntireWsAreUsed() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC1_LOCATION, "fed1");
        Session ws1Session = jcrSessionTo("ws1");
        try {
            ws1Session.getWorkspace().clone("default", "/testRoot", "/testRoot", true);
            fail("Should only be able to clone between workspaces if the entire workspace is used");
        } catch (RepositoryException e) {
            // expected
            if (print) {
                e.printStackTrace();
            }
        } finally {
            ws1Session.logout();
        }
    }

    @Test
    @FixFor( "MODE-1975" )
    public void shouldCloneEntireWorkspaces() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "fed2");
        Session ws1Session = jcrSessionTo("ws1");
        try {
            ws1Session.getWorkspace().clone("default", "/", "/", true);
            assertNodeFound("/testRoot", ws1Session);
            Node fed2 = assertNodeFound("/testRoot/fed2", ws1Session);
            assertNodeFound("/testRoot/fed2/federated3", ws1Session);

            // add an external node in the 2nd workspace and check that it was added via the connector (i.e. the projection was
            // correctly cloned)
            fed2.addNode("federated2_1");
            ws1Session.save();
            //sleep a bit to make sure the events which clear the ws cache have reached the other session
            Thread.sleep(100L);
            assertNodeFound("/testRoot/fed2/federated2_1");
        } finally {
            ws1Session.logout();
        }
    }

    @Test
    @FixFor( "MODE-1975" )
    public void shouldNotCloneEntireWorkspacesIfExternalNodesExist() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "fed2");
        Session ws1Session = jcrSessionTo("ws1");
        ws1Session.getRootNode().addNode("testRoot");
        ws1Session.save();
        ((Workspace)ws1Session.getWorkspace()).getFederationManager().createProjection("/testRoot", SOURCE_NAME,
                                                                                       MockConnector.DOC2_LOCATION, "ws1Fed2");
        try {
            ws1Session.getWorkspace().clone("default", "/", "/", false);
            fail("Expected an ItemExistsException because the target workspace already contains an external node");
        } catch (ItemExistsException e) {
            // expected
            if (print) {
                e.printStackTrace();
            }
        } finally {
            ws1Session.logout();
        }
    }

    @Test
    public void shouldValidatePermissionsForReadonlyProjections() throws Exception {
        federationManager.createProjection("/testRoot", "mock-source-readonly", MockConnector.DOC1_LOCATION, "fed1");
        federationManager.createProjection("/testRoot", "mock-source-readonly", MockConnector.DOC2_LOCATION, "fed2");

        assertPermission(true, "/testRoot", ModeShapePermissions.ADD_NODE);
        assertPermission(true, "/testRoot", ModeShapePermissions.ADD_NODE, ModeShapePermissions.READ);
        assertPermission(true, "/testRoot", ModeShapePermissions.READ);

        assertPermission(false, "/testRoot/fed1", ModeShapePermissions.ADD_NODE);
        assertPermission(false, "/testRoot/fed1", ModeShapePermissions.ADD_NODE, ModeShapePermissions.READ);
        assertPermission(true, "/testRoot/fed1", ModeShapePermissions.READ);
        assertPermission(true, "/testRoot/fed1", ModeShapePermissions.INDEX_WORKSPACE);
        assertPermission(true, "/testRoot/fed1", ModeShapePermissions.INDEX_WORKSPACE, ModeShapePermissions.READ);

        assertPermission(false, "/testRoot/fed2", ModeShapePermissions.ADD_NODE);
        assertPermission(false, "/testRoot/fed2", ModeShapePermissions.ADD_NODE, ModeShapePermissions.READ);
        assertPermission(true, "/testRoot/fed2", ModeShapePermissions.READ);
        assertPermission(true, "/testRoot/fed2", ModeShapePermissions.INDEX_WORKSPACE);
        assertPermission(true, "/testRoot/fed2", ModeShapePermissions.INDEX_WORKSPACE, ModeShapePermissions.READ);

        assertPermission(false, "/testRoot/fed2/federated3", ModeShapePermissions.ADD_NODE);
        assertPermission(false, "/testRoot/fed2/federated3", ModeShapePermissions.ADD_NODE, ModeShapePermissions.READ);
        assertPermission(true, "/testRoot/fed2/federated3", ModeShapePermissions.READ);
        assertPermission(true, "/testRoot/fed2/federated3", ModeShapePermissions.INDEX_WORKSPACE);
        assertPermission(true, "/testRoot/fed2/federated3", ModeShapePermissions.INDEX_WORKSPACE, ModeShapePermissions.READ);
    }

    @Test
    @FixFor( "MODE-2147" )
    public void shouldNotAllowVersionableMixinOnExternalNodes() throws Exception {
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "fed1");
        Node projectionRoot = session.getNode("/testRoot/fed1");
        try {
            projectionRoot.addMixin("mix:versionable");
            fail("Should not allow versionable mixin on external nodes");
        } catch (ConstraintViolationException e) {
            // expected
        }

        try {
            Node externalChild = projectionRoot.addNode("child");
            externalChild.addMixin("mix:versionable");
            session.save();
            fail("Should not allow versionable mixin on external nodes");
        } catch (ConstraintViolationException e) {
            // expected
        }

        Node externalChild = projectionRoot.addNode("child");
        assertThat(externalChild, is(notNullValue()));
        session.save();
        try {
            ((Node)session.getNode("/testRoot/child")).addMixin("mix:versionable");
            fail("Should not allow versionable mixin on external nodes");
        } catch (RepositoryException e) {
            // expected
        }
    }

    @Test
    public void shouldNotAllowACLsOnExternalNodes() throws Exception {
        AccessControlManager acm = session.getAccessControlManager();
        federationManager.createProjection("/testRoot", SOURCE_NAME, MockConnector.DOC2_LOCATION, "fed1");
        session.getNode("/testRoot/fed1");
        AccessControlList acl = acl("/testRoot/fed1");

        try {
            acm.setPolicy("/testRoot/fed1", acl);
            fail("Should not allow ACLs on external nodes");
        } catch (RepositoryException e) {
            // expected
        }
    }

    private void assertPermission( boolean shouldHave,
                                   String absPath,
                                   String... actions ) throws RepositoryException {
        StringBuilder actionsBuilder = new StringBuilder();
        List<String> actionsList = new ArrayList<String>(Arrays.asList(actions));
        for (Iterator<String> actionsIterator = actionsList.iterator(); actionsIterator.hasNext();) {
            actionsBuilder.append(actionsIterator.next());
            if (actionsIterator.hasNext()) {
                actionsBuilder.append(",");
            }
        }
        String actionsString = actionsBuilder.toString();
        if (shouldHave) {
            assertTrue(session.hasPermission(absPath, actionsString));
            session.checkPermission(absPath, actionsString);
        } else {
            assertFalse(session.hasPermission(absPath, actionsString));
            try {
                session.checkPermission(absPath, actionsString);
                fail("There permissions " + actionsString + " should not be valid on " + absPath);
            } catch (AccessControlException e) {
                // expected
            }
        }
    }

    private void assertExternalNodeHasChildren( String externalNodePath,
                                                String... children ) throws Exception {
        Node externalNode = session.getNode(externalNodePath);
        NodeIterator childNodes = externalNode.getNodes();

        if (children.length == 0) {
            assertEquals(0, childNodes.getSize());
            return;
        }
        List<String> actualNodes = new ArrayList<String>((int)childNodes.getSize());
        while (childNodes.hasNext()) {
            actualNodes.add(childNodes.nextNode().getName());
        }

        assertEquals(Arrays.asList(children), actualNodes);
    }

    private void assertNodeNotFound( String absPath ) throws RepositoryException {
        try {
            session.getNode(absPath);
            fail("Node at " + absPath + " should not exist");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    private Node assertNodeFound( String absPath ) throws RepositoryException {
        return assertNodeFound(absPath, session);
    }

    private Node assertNodeFound( String absPath,
                                  Session session ) throws RepositoryException {
        Node node = session.getNode(absPath);
        assertNotNull(node);
        return node;
    }

    private Session jcrSessionTo( String wsName ) throws RepositoryException {
        return repository.login(wsName);
    }

}
