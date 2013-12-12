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
package org.modeshape.jcr;

import static com.mongodb.util.MyAsserts.assertNotEquals;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.junit.SkipLongRunning;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.WorkspaceRemoved;

/**
 * @author jverhaeg
 */
public class JcrWorkspaceTest extends SingleUseAbstractTest {

    private JcrWorkspace workspace;
    private String workspaceName;

    private JcrSession otherSession;
    private JcrWorkspace otherWorkspace;
    private String otherWorkspaceName;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

        Node root = session.getRootNode();
        Node a = root.addNode("a");
        Node ab = a.addNode("b", "nt:unstructured");
        Node abc = ab.addNode("c");
        Node b = root.addNode("b");
        abc.setProperty("stringProperty", "value");
        session.save();
        assertThat(b, is(notNullValue()));

        workspace = session.getWorkspace();
        workspaceName = workspace.getName();

        otherWorkspaceName = "anotherWs";
        workspace.createWorkspace(otherWorkspaceName);
        otherSession = repository.login(otherWorkspaceName);
        otherWorkspace = otherSession.getWorkspace();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCloneWithNullWorkspaceName() throws Exception {
        workspace.clone(null, "/src", "/dest", false);
    }

    @Test
    @FixFor( "MODE-1972" )
    public void shouldCloneEntireWorkspaces() throws Exception {
        otherWorkspace.clone(workspaceName, "/", "/", true);

        assertEquals(session.getNode("/a").getIdentifier(), otherSession.getNode("/a").getIdentifier());
        assertEquals(session.getNode("/a/b").getIdentifier(), otherSession.getNode("/a/b").getIdentifier());
        assertEquals(session.getNode("/a/b/c").getIdentifier(), otherSession.getNode("/a/b/c").getIdentifier());
        assertEquals(session.getNode("/b").getIdentifier(), otherSession.getNode("/b").getIdentifier());
    }

    @Test( expected = RepositoryException.class )
    @FixFor( "MODE-1972" )
    public void shouldNotClonePartialWorkspaceIntoWorkspaceRoot() throws Exception {
        otherWorkspace.clone(workspaceName, "/a/b", "/", false);
    }

    @Test
    @FixFor( "MODE-2007" )
    public void shouldCloneChildrenOfRoot() throws Exception {
        otherWorkspace.clone(workspaceName, "/a", "/a", false);
        otherWorkspace.clone(workspaceName, "/b", "/b", false);

        assertEquals(session.getNode("/a").getIdentifier(), otherSession.getNode("/a").getIdentifier());
        assertEquals(session.getNode("/a/b").getIdentifier(), otherSession.getNode("/a/b").getIdentifier());
        assertEquals(session.getNode("/a/b/c").getIdentifier(), otherSession.getNode("/a/b/c").getIdentifier());
        assertEquals(session.getNode("/b").getIdentifier(), otherSession.getNode("/b").getIdentifier());
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCopyFromNullPathToNullPath() throws Exception {
        workspace.copy(null, null);
    }

    @Test
    public void shouldCopyFromPathToAnotherPathInSameWorkspace() throws Exception {
        workspace.copy("/a/b", "/b/b-copy");
    }

    @Test
    @FixFor( "MODE-1972" )
    public void shouldCopyEntireWorkspaces() throws Exception {
        otherWorkspace.copy(workspaceName, "/", "/");

        assertNotNull(otherSession.getNode("/a"));
        assertNotNull(otherSession.getNode("/a/b"));
        assertNotNull(otherSession.getNode("/a/b/c"));
        assertNotNull(otherSession.getNode("/b"));
    }

    @Test( expected = RepositoryException.class )
    @FixFor( "MODE-1972" )
    public void shouldNotCopyPartialWorkspaceIntoWorkspaceRoot() throws Exception {
        otherWorkspace.copy(workspaceName, "/a/b", "/");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCopyFromOtherWorkspaceWithNullWorkspace() throws Exception {
        workspace.copy(null, null, null);
    }

    @Test
    public void shouldAllowGetAccessibleWorkspaceNames() throws Exception {
        List<String> names = Arrays.asList(workspace.getAccessibleWorkspaceNames());
        assertThat(names.size(), is(2));
        assertThat(names.contains(workspaceName), is(true));
        assertThat(names.contains(otherWorkspaceName), is(true));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowImportContentHandlerWithNullPath() throws Exception {
        workspace.getImportContentHandler(null, 0);
    }

    @Test
    public void shouldGetImportContentHandlerWithValidPath() throws Exception {
        assertThat(workspace.getImportContentHandler("/b", 0), is(notNullValue()));
    }

    @Test
    public void shouldProvideName() throws Exception {
        assertThat(workspace.getName(), is(workspaceName));
    }

    @Test
    public void shouldHaveSameContextIdAsSession() {
        assertThat(workspace.context().getId(), is(session.context().getId()));
    }

    @Test
    public void shouldProvideNamespaceRegistry() throws Exception {
        NamespaceRegistry registry = workspace.getNamespaceRegistry();
        assertThat(registry, is(notNullValue()));
        assertThat(registry.getURI(JcrLexicon.Namespace.PREFIX), is(JcrLexicon.Namespace.URI));
    }

    @Test
    public void shouldGetNodeTypeManager() throws Exception {
        assertThat(workspace.getNodeTypeManager(), is(notNullValue()));
    }

    @Test
    public void shouldGetObservationManager() throws Exception {
        assertThat(workspace.getObservationManager(), is(notNullValue()));
    }

    @Test
    public void shouldProvideQueryManager() throws Exception {
        assertThat(workspace.getQueryManager(), notNullValue());
    }

    @Test
    public void shouldCreateQuery() throws Exception {
        String statement = "SELECT * FROM [nt:unstructured]";

        QueryManager queryManager = workspace.getQueryManager();
        Query query = queryManager.createQuery(statement, Query.JCR_SQL2);

        assertThat(query, is(notNullValue()));
        assertThat(query.getLanguage(), is(Query.JCR_SQL2));
        assertThat(query.getStatement(), is(statement));
    }

    @Test
    public void shouldStoreQueryAsNode() throws Exception {
        String statement = "SELECT * FROM [nt:unstructured]";

        QueryManager queryManager = workspace.getQueryManager();
        Query query = queryManager.createQuery(statement, Query.JCR_SQL2);

        Node node = query.storeAsNode("/storedQuery");
        assertThat(node, is(notNullValue()));
        assertThat(node.getPrimaryNodeType().getName(), is("nt:query"));
        assertThat(node.getProperty("jcr:language").getString(), is(Query.JCR_SQL2));
        assertThat(node.getProperty("jcr:statement").getString(), is(statement));
    }

    @Test
    public void shouldLoadStoredQuery() throws Exception {
        String statement = "SELECT * FROM [nt:unstructured]";

        QueryManager queryManager = workspace.getQueryManager();
        Query query = queryManager.createQuery(statement, Query.JCR_SQL2);

        Node node = query.storeAsNode("/storedQuery");

        Query loaded = queryManager.getQuery(node);

        assertThat(loaded, is(notNullValue()));
        assertThat(loaded.getLanguage(), is(Query.JCR_SQL2));
        assertThat(loaded.getStatement(), is(statement));
        assertThat(loaded.getStoredQueryPath(), is(node.getPath()));
    }

    @Test
    public void shouldProvideSession() throws Exception {
        assertThat(workspace.getSession(), is(notNullValue()));
    }

    @Test
    public void shouldAllowImportXml() throws Exception {
        String inputData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                           + "<sv:node xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" "
                           + "xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" sv:name=\"workspaceTestNode\">"
                           + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">"
                           + "<sv:value>nt:unstructured</sv:value></sv:property></sv:node>";
        workspace.importXML("/", new ByteArrayInputStream(inputData.getBytes()), 0);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowMoveFromNullPath() throws Exception {
        workspace.move(null, null);
    }

    @Test
    public void shouldAllowMoveFromPathToAnotherPathInSameWorkspace() throws Exception {
        workspace.move("/a/b", "/b/b-copy");
    }

    @Test
    @FixFor( "MODE-2009" )
    public void shouldRemoveAllNodesWhenRemovingWorkspace() throws Exception {
        final String wsName = "testWS";
        workspace.createWorkspace(wsName);
        JcrSession testWsSession = repository.login(wsName);
        testWsSession.getRootNode().addNode("testRoot").addNode("subNode");
        assertNotNull(testWsSession.getNode("/jcr:system"));
        testWsSession.save();
        testWsSession.logout();

        // workspace deletion clears the cache asynchronously so we need to wait until that completes
        final CountDownLatch workspaceDeletedLatch = new CountDownLatch(1);
        repository.repositoryCache().register(new ChangeSetListener() {
            @Override
            public void notify( ChangeSet changeSet ) {
                for (Change change : changeSet) {
                    if (change instanceof WorkspaceRemoved && ((WorkspaceRemoved)change).getWorkspaceName().equals(wsName)) {
                        try {
                            // we know the ws removed event has been issued, but we need to wait to make sure ISPN has finished
                            // shutting down the ws cache
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        workspaceDeletedLatch.countDown();
                    }
                }
            }
        });
        workspace.deleteWorkspace(wsName);
        workspaceDeletedLatch.await(10, TimeUnit.SECONDS);

        workspace.createWorkspace(wsName);
        testWsSession = repository.login(wsName);
        assertNotNull(testWsSession.getNode("/jcr:system"));
        assertNotFound("/testRoot/subNode", testWsSession);
        assertNotFound("/testRoot", testWsSession);
        assertEquals(1, testWsSession.getRootNode().getNodes().getSize());

        assertNotNull(session.getNode("/jcr:system"));
        testWsSession.logout();
    }

    @Test
    @FixFor( "MODE-2012" )
    public void clonedReferencesShouldPointToTargetWorkspace() throws Exception {
        tools.registerNodeTypes(session, "cnd/references.cnd");

        Node nodeA = session.getRootNode().addNode("A", "test:node");
        Node nodeB = session.getRootNode().addNode("B", "test:node");
        Node nodeC = session.getRootNode().addNode("C", "test:node");
        Node nodeD = session.getRootNode().addNode("D", "test:node");

        nodeA.setProperty("test:strongReference", session.getValueFactory().createValue(nodeB));
        nodeA.setProperty("test:weakReference", session.getValueFactory().createValue(nodeC, true));
        nodeA.setProperty("test:simpleReference", session.getValueFactory().createSimpleReference(nodeD));

        session.save();

        otherWorkspace.clone("default", "/", "/", true);

        nodeD.remove();
        nodeC.remove();
        nodeB.remove();
        nodeA.remove();

        session.save();

        AbstractJcrNode otherA = otherSession.getNode("/A");
        AbstractJcrNode otherB = otherSession.getNode("/B");
        AbstractJcrNode otherC = otherSession.getNode("/C");
        AbstractJcrNode otherD = otherSession.getNode("/D");

        assertEquals(otherB.getIdentifier(), otherA.getProperty("test:strongReference").getNode().getIdentifier());
        assertEquals(otherC.getIdentifier(), otherA.getProperty("test:weakReference").getNode().getIdentifier());
        assertEquals(otherD.getIdentifier(), otherA.getProperty("test:simpleReference").getNode().getIdentifier());
    }

    @Test
    @FixFor( "MODE-2012" )
    public void clonedUUIDsShouldBeTheSame() throws Exception {
        tools.registerNodeTypes(session, "cnd/references.cnd");

        Node nodeA = session.getRootNode().addNode("A", "test:node");
        Node nodeB = session.getRootNode().addNode("B", "test:node");
        session.save();

        otherWorkspace.clone("default", "/", "/", true);

        assertEquals(nodeA.getIdentifier(), otherSession.getNode("/A").getIdentifier());
        assertEquals(nodeB.getIdentifier(), otherSession.getNode("/B").getIdentifier());
    }

    @Test
    @FixFor( "MODE-2012" )
    public void copiedReferencesShouldPointToTargetWorkspace() throws Exception {
        tools.registerNodeTypes(session, "cnd/references.cnd");

        Node nodeA = session.getRootNode().addNode("A", "test:node");
        Node nodeB = session.getRootNode().addNode("B", "test:node");
        Node nodeC = session.getRootNode().addNode("C", "test:node");
        Node nodeD = session.getRootNode().addNode("D", "test:node");

        nodeA.setProperty("test:strongReference", session.getValueFactory().createValue(nodeB));
        nodeA.setProperty("test:weakReference", session.getValueFactory().createValue(nodeC, true));
        nodeA.setProperty("test:simpleReference", session.getValueFactory().createSimpleReference(nodeD));

        session.save();

        otherWorkspace.copy("default", "/", "/");

        nodeD.remove();
        nodeC.remove();
        nodeB.remove();
        nodeA.remove();
        session.save();

        AbstractJcrNode otherA = otherSession.getNode("/A");
        AbstractJcrNode otherB = otherSession.getNode("/B");
        AbstractJcrNode otherC = otherSession.getNode("/C");
        AbstractJcrNode otherD = otherSession.getNode("/D");

        assertEquals(otherB.getIdentifier(), otherA.getProperty("test:strongReference").getNode().getIdentifier());
        assertEquals(otherC.getIdentifier(), otherA.getProperty("test:weakReference").getNode().getIdentifier());
        assertEquals(otherD.getIdentifier(), otherA.getProperty("test:simpleReference").getNode().getIdentifier());
    }

    @Test
    @FixFor( "MODE-2012" )
    public void copiedUUIDsShouldNotBeTheSame() throws Exception {
        tools.registerNodeTypes(session, "cnd/references.cnd");

        Node nodeA = session.getRootNode().addNode("A", "test:node");
        Node nodeB = session.getRootNode().addNode("B", "test:node");
        session.save();

        otherWorkspace.copy("default", "/", "/");

        assertNotEquals(nodeA.getIdentifier(), otherSession.getNode("/A").getIdentifier());
        assertNotEquals(nodeB.getIdentifier(), otherSession.getNode("/B").getIdentifier());
    }

    @Test
    @FixFor( "MODE-2114" )
    public void copiedReferencesShouldHaveUpdatedUUIDs() throws Exception {
        tools.registerNodeTypes(session, "cnd/references.cnd");

        Node parent = session.getRootNode().addNode("parent");
        Node nodeA = parent.addNode("A", "test:node");
        Node nodeB = parent.addNode("B", "test:node");
        Node nodeC = parent.addNode("C", "test:node");
        Node nodeD = parent.addNode("D", "test:node");

        nodeA.setProperty("test:strongReference", session.getValueFactory().createValue(nodeB));
        nodeA.setProperty("test:weakReference", session.getValueFactory().createValue(nodeC, true));
        nodeA.setProperty("test:simpleReference", session.getValueFactory().createSimpleReference(nodeD));

        session.save();

        workspace.copy("/parent", "/new_parent");

        AbstractJcrNode otherA = session.getNode("/new_parent/A");
        AbstractJcrNode otherB = session.getNode("/new_parent/B");
        AbstractJcrNode otherC = session.getNode("/new_parent/C");
        AbstractJcrNode otherD = session.getNode("/new_parent/D");

        assertEquals(otherB.getIdentifier(), otherA.getProperty("test:strongReference").getNode().getIdentifier());
        assertEquals(otherC.getIdentifier(), otherA.getProperty("test:weakReference").getNode().getIdentifier());
        assertEquals(otherD.getIdentifier(), otherA.getProperty("test:simpleReference").getNode().getIdentifier());
    }

    @Test
    @FixFor( "MODE-2115" )
    public void copiedNodesShouldReplaceAutoCreatedChildren() throws Exception {
        tools.registerNodeTypes(session, "cnd/autocreated-child-nodes.cnd");

        Node parent = session.getRootNode().addNode("parent", "test:autocreatedFolders");
        session.save();

        long folder1CreatedTs = parent.getNode("folder1").getProperty("jcr:created").getDate().getTimeInMillis();
        long folder2CreatedTs = parent.getNode("folder2").getProperty("jcr:created").getDate().getTimeInMillis();

        workspace.copy("/parent", "/new_parent");

        AbstractJcrNode newParent = session.getNode("/new_parent");
        assertEquals(2, newParent.getNodes().getSize());
        Node folder1Copy = assertNode("/new_parent/folder1", "nt:folder");
        assertEquals(folder1CreatedTs, folder1Copy.getProperty("jcr:created").getDate().getTimeInMillis());
        Node folder2Copy = assertNode("/new_parent/folder2", "nt:folder");
        assertEquals(folder2CreatedTs, folder2Copy.getProperty("jcr:created").getDate().getTimeInMillis());
    }

    @Test
    @FixFor( "MODE-2115" )
    public void copiedNodesShouldReplaceReferenceableAutoCreatedChildren() throws Exception {
        tools.registerNodeTypes(session, "cnd/autocreated-child-nodes.cnd");

        session.getRootNode().addNode("parent", "test:autocreatedReferenceableChildren");
        session.save();

        session.getNode("/parent/child1").addNode("child1_1");
        session.save();

        workspace.copy("/parent", "/new_parent");

        AbstractJcrNode newParent = session.getNode("/new_parent");
        assertEquals(2, newParent.getNodes().getSize());
        //validate that the jcr:uuid is the same as the identifier of the node
        Node child1 = assertNode("/new_parent/child1", "test:subnodeReferenceable");
        assertEquals(child1.getIdentifier(), child1.getProperty("jcr:uuid").getString());

        Node child2 = assertNode("/new_parent/child2", "test:subnodeReferenceable");
        assertEquals(child2.getIdentifier(), child2.getProperty("jcr:uuid").getString());

        assertNode("/new_parent/child1/child1_1");
    }

    @Test
    @FixFor( "MODE-2115" )
    public void clonedNodesWithAutoCreatedChildrenShouldPreserveIdentifiers() throws Exception {
        tools.registerNodeTypes(session, "cnd/autocreated-child-nodes.cnd");

        Node parent = session.getRootNode().addNode("parent", "test:autocreatedFolders");
        session.save();

        Node folder1 = parent.getNode("folder1");
        long folder1CreatedTs = folder1.getProperty("jcr:created").getDate().getTimeInMillis();
        Node folder2 = parent.getNode("folder2");
        long folder2CreatedTs = folder2.getProperty("jcr:created").getDate().getTimeInMillis();

        otherWorkspace.clone(workspaceName, "/parent", "/parent", true);

        AbstractJcrNode otherParent = otherSession.getNode("/parent");
        assertEquals(2, otherParent.getNodes().getSize());
        Node otherFolder1 = otherSession.getNode("/parent/folder1");
        assertEquals(folder1CreatedTs, otherFolder1.getProperty("jcr:created").getDate().getTimeInMillis());
        assertEquals(folder1.getIdentifier(), otherFolder1.getIdentifier());
        Node otherFolder2 = otherSession.getNode("/parent/folder2");
        assertEquals(folder2.getIdentifier(), otherFolder2.getIdentifier());
        assertEquals(folder2CreatedTs, otherFolder2.getProperty("jcr:created").getDate().getTimeInMillis());
    }

    @SkipLongRunning
    @FixFor( "MODE-2012" )
    @Test
    public void shouldCorrectlyImportSameContentIntoMultipleWorkspaces() throws Exception {
        // Register the Cars node types used in the content ...
        tools.registerNodeTypes(session, "cars.cnd");

        createWorkspace("workspaceA");
        createWorkspace("workspaceB");
        createWorkspace("workspaceC");
        importFile("/", "workspaceA", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        importFile("/", "workspaceB", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        importFile("/", "workspaceC", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

        // print = true;
        printWorkspace("workspaceA", false);
        printWorkspace("workspaceB", false);
        printWorkspace("workspaceC", false);

        checkCorrespondingPaths("workspaceA", "workspaceB", "workspaceC");
        checkCorrespondingPaths("workspaceB", "workspaceA", "workspaceC");
        checkCorrespondingPaths("workspaceC", "workspaceA", "workspaceB");

        Session sessionA = repository.login("workspaceA");
        Session sessionB = repository.login("workspaceB");
        Session sessionC = repository.login("workspaceC");

        for (int i = 0; i != 3; ++i) {

            Node utilityA = sessionA.getNode("/Cars/Utility");
            Node utilityB = sessionB.getNode("/Cars/Utility");
            Node utilityC = sessionC.getNode("/Cars/Utility");

            NodeKey utilityAkey = ((AbstractJcrNode)utilityA).key();
            NodeKey utilityBkey = ((AbstractJcrNode)utilityA).key();
            NodeKey utilityCkey = ((AbstractJcrNode)utilityA).key();
            final String sourceKey = utilityAkey.getSourceKey();
            assertThat(utilityAkey.getSourceKey(), is(utilityBkey.getSourceKey()));
            assertThat(utilityAkey.getSourceKey(), is(utilityCkey.getSourceKey()));

            NodeIterator iterA = utilityA.getNodes();
            NodeIterator iterB = utilityB.getNodes();
            NodeIterator iterC = utilityC.getNodes();
            while (iterA.hasNext()) {
                Node childA = iterA.nextNode();
                Node childB = iterB.nextNode();
                Node childC = iterC.nextNode();
                assertThat(childA.getName(), is(childB.getName()));
                assertThat(childA.getName(), is(childC.getName()));
                if (i != 0 && childA.getName().startsWith("newChild")) {
                    // Must be our new node ...
                    continue;
                }
                assertThat(childA.isNodeType("mix:referenceable"), is(true));
                assertThat(childB.isNodeType("mix:referenceable"), is(true));
                assertThat(childC.isNodeType("mix:referenceable"), is(true));
                NodeKey childAkey = ((AbstractJcrNode)childA).key();
                NodeKey childBkey = ((AbstractJcrNode)childB).key();
                NodeKey childCkey = ((AbstractJcrNode)childC).key();
                assertThat(childAkey.getSourceKey(), is(sourceKey));
                assertThat(childBkey.getSourceKey(), is(sourceKey));
                assertThat(childCkey.getSourceKey(), is(sourceKey));
                assertThat(childBkey.getWorkspaceKey(), is(not(childAkey.getWorkspaceKey())));
                assertThat(childCkey.getWorkspaceKey(), is(not(childAkey.getWorkspaceKey())));
                assertThat(childBkey.getIdentifier(), is(childAkey.getIdentifier()));
                assertThat(childCkey.getIdentifier(), is(childAkey.getIdentifier()));
            }

            // Now add a new non-referencable child to "/Cars/Utility" ...
            Node childA = utilityA.addNode("newChild" + (i + 1));
            Node childB = utilityB.addNode("newChild" + (i + 1));
            Node childC = utilityC.addNode("newChild" + (i + 1));
            childA.getSession().save();
            childB.getSession().save();
            childC.getSession().save();
        }

        checkCorrespondingPaths("workspaceA", "workspaceB", "workspaceC");
        checkCorrespondingPaths("workspaceB", "workspaceA", "workspaceC");
        checkCorrespondingPaths("workspaceC", "workspaceA", "workspaceB");

        sessionA.logout();
        sessionB.logout();
        sessionC.logout();
    }

    @SkipLongRunning
    @FixFor( "MODE-2012" )
    @Test
    public void shouldCorrectlyCloneWorkspacesWithCorrespondingContent() throws Exception {
        // Register the Cars node types used in the content ...
        tools.registerNodeTypes(session, "cars.cnd");

        createWorkspace("workspaceA");
        createWorkspace("workspaceB");
        createWorkspace("workspaceC");
        importFile("/", "workspaceA", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        cloneWorkspace("workspaceA", "workspaceB", false);
        cloneWorkspace("workspaceA", "workspaceC", false);

        // print = true;
        printWorkspace("workspaceA", false);
        printWorkspace("workspaceB", false);
        printWorkspace("workspaceC", false);

        checkCorrespondingPaths("workspaceA", "workspaceB", "workspaceC");
        checkCorrespondingPaths("workspaceB", "workspaceA", "workspaceC");
        checkCorrespondingPaths("workspaceC", "workspaceA", "workspaceB");

        Session sessionA = repository.login("workspaceA");
        Session sessionB = repository.login("workspaceB");
        Session sessionC = repository.login("workspaceC");

        for (int i = 0; i != 3; ++i) {

            Node utilityA = sessionA.getNode("/Cars/Utility");
            Node utilityB = sessionB.getNode("/Cars/Utility");
            Node utilityC = sessionC.getNode("/Cars/Utility");

            NodeKey utilityAkey = ((AbstractJcrNode)utilityA).key();
            NodeKey utilityBkey = ((AbstractJcrNode)utilityA).key();
            NodeKey utilityCkey = ((AbstractJcrNode)utilityA).key();
            final String sourceKey = utilityAkey.getSourceKey();
            assertThat(utilityAkey.getSourceKey(), is(utilityBkey.getSourceKey()));
            assertThat(utilityAkey.getSourceKey(), is(utilityCkey.getSourceKey()));

            NodeIterator iterA = utilityA.getNodes();
            NodeIterator iterB = utilityB.getNodes();
            NodeIterator iterC = utilityC.getNodes();
            while (iterA.hasNext()) {
                Node childA = iterA.nextNode();
                if (childA.getName().startsWith("newChild")) {
                    assertThat(iterB.hasNext(), is(false));
                    assertThat(iterC.hasNext(), is(false));
                    continue;
                }
                Node childB = iterB.nextNode();
                Node childC = iterC.nextNode();
                assertThat(childA.getName(), is(childB.getName()));
                assertThat(childA.getName(), is(childC.getName()));
                assertThat(childA.isNodeType("mix:referenceable"), is(true));
                assertThat(childB.isNodeType("mix:referenceable"), is(true));
                assertThat(childC.isNodeType("mix:referenceable"), is(true));
                NodeKey childAkey = ((AbstractJcrNode)childA).key();
                NodeKey childBkey = ((AbstractJcrNode)childB).key();
                NodeKey childCkey = ((AbstractJcrNode)childC).key();
                assertThat(childAkey.getSourceKey(), is(sourceKey));
                assertThat(childBkey.getSourceKey(), is(sourceKey));
                assertThat(childCkey.getSourceKey(), is(sourceKey));
                assertThat(childBkey.getWorkspaceKey(), is(not(childAkey.getWorkspaceKey())));
                assertThat(childCkey.getWorkspaceKey(), is(not(childAkey.getWorkspaceKey())));
                assertThat(childBkey.getIdentifier(), is(childAkey.getIdentifier()));
                assertThat(childCkey.getIdentifier(), is(childAkey.getIdentifier()));
            }

            // Now add a new non-referencable child to "/Cars/Utility" in workspace A only ...
            Node childA = utilityA.addNode("newChild" + (i + 1));
            childA.getSession().save();
        }

        checkCorrespondingPaths("workspaceA", "workspaceB", "workspaceC");
        checkCorrespondingPaths("workspaceB", "workspaceA", "workspaceC");
        checkCorrespondingPaths("workspaceC", "workspaceA", "workspaceB");

        sessionA.logout();
        sessionB.logout();
        sessionC.logout();

    }

    protected void checkCorrespondingPaths( String workspace,
                                            final String... otherWorkspaces ) throws Exception {
        final Session session = repository.login(workspace);
        final Map<String, Session> otherWorkspaceSessions = new HashMap<String, Session>();
        for (String otherWorkspace : otherWorkspaces) {
            assertThat(otherWorkspace, is(not(workspace)));
            Session otherSession = repository.login(otherWorkspace);
            otherWorkspaceSessions.put(otherWorkspace, otherSession);
        }
        final String expectedWorkspaceKey = ((AbstractJcrNode)session.getRootNode()).key().getWorkspaceKey();

        try {
            tools.onEachNode(session, false, new JcrTools.NodeOperation() {
                @Override
                public void run( Node node ) throws Exception {
                    final String path = node.getPath();
                    final NodeKey key = ((AbstractJcrNode)node).key();
                    assertThat(key.getWorkspaceKey(), is(expectedWorkspaceKey));
                    if (node.isNodeType("mix:referenceable")) {
                        for (String otherWorkspace : otherWorkspaces) {
                            String correspondingPath = node.getCorrespondingNodePath(otherWorkspace);
                            assertThat(correspondingPath, is(path));
                            // Check that the node keys are actually different ...
                            final Node correspondingNode = otherWorkspaceSessions.get(otherWorkspace).getNode(correspondingPath);
                            final NodeKey correspondingKey = ((AbstractJcrNode)correspondingNode).key();
                            assertThat(correspondingKey, is(not(key)));
                            assertThat(correspondingKey.getIdentifier(), is(key.getIdentifier()));
                            assertThat(correspondingKey.getSourceKey(), is(key.getSourceKey()));
                            assertThat(correspondingKey.getWorkspaceKey(), is(not(key.getWorkspaceKey()))); // this is what
                                                                                                            // differs
                        }
                    }
                }
            });
        } finally {
            session.logout();
            for (Session otherSession : otherWorkspaceSessions.values()) {
                otherSession.logout();
            }
        }
    }

    protected void printWorkspace( String workspace,
                                   boolean includeSystem ) throws Exception {
        printWorkspace(workspace, includeSystem, Integer.MAX_VALUE);
    }

    protected void printWorkspace( String workspaceName,
                                   boolean includeSystem,
                                   int maxDepthToPrint ) throws Exception {
        if (!print) return;
        Session session = repository.login(workspaceName);
        try {
            printMessage("Content of workspace '" + workspaceName + "'");
            NodeIterator iter = session.getRootNode().getNodes();
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (!includeSystem && node.getName().equals("jcr:system")) continue;
                tools.printSubgraph(node, " ", 1, maxDepthToPrint);
            }
        } finally {
            session.logout();
        }
    }

    protected void createWorkspace( String workspaceName ) throws Exception {
        Session session = repository.login();
        try {
            session.getWorkspace().createWorkspace(workspaceName);
        } finally {
            session.logout();
        }
    }

    protected void cloneWorkspace( String sourceWorkspace,
                                   String targetWorkspace,
                                   boolean removeExisting ) throws Exception {
        Session session = repository.login(targetWorkspace);
        try {
            session.getWorkspace().clone(sourceWorkspace, "/", "/", removeExisting);
        } finally {
            session.logout();
        }
    }

    protected void importFile( String importIntoPath,
                               String workspaceName,
                               String resourceName,
                               int importBehavior ) throws Exception {
        // Import the car content ...
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName);
        assertThat(stream, is(notNullValue()));
        Session session = repository.login(workspaceName);
        try {
            session.getWorkspace().importXML(importIntoPath, stream, importBehavior); // shouldn't exist yet
        } finally {
            try {
                stream.close();
            } finally {
                session.logout();
            }
        }
    }

    private void assertNotFound( String absPath,
                                 JcrSession jcrSession ) throws RepositoryException {
        try {
            jcrSession.getNode(absPath);
            fail("Node " + absPath + " should not have been found in the session " + session);
        } catch (PathNotFoundException e) {
            // expected
        }
    }
}
