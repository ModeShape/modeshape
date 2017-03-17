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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.query.Query;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import javax.transaction.TransactionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.connector.mock.MockConnector;
import org.modeshape.jcr.api.BackupOptions;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.api.Problems;
import org.modeshape.jcr.api.RestoreOptions;
import org.modeshape.jcr.api.Workspace;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.security.SimplePrincipal;
import org.modeshape.jcr.value.Path;

public class ModeshapePersistenceIT {

    private static final String[] BINARY_RESOURCES = new String[] { "data/large-file1.png", 
                                                                    "data/move-initial-data.xml", 
                                                                    "data/simple.json", 
                                                                    "data/singleNode.json" };
   
    private final TestRepository testRepository = new TestRepository();

    private File backupArea;
    private File backupDirectory;
    private File backupDirectory2;
    private File backupRepoDir;

    @Before
    public void beforeEach() throws Exception {
        backupArea = new File("target/backupArea");
        FileUtil.delete(backupArea.getPath());
        //use a UUID for the backup folder to prevent some file locks lingering between tests
        String folderId = UUID.randomUUID().toString();
        backupDirectory = new File(backupArea, "repoBackups_" + folderId);
        backupDirectory2 = new File(backupArea, "repoBackupsAfter_" + folderId);
        backupDirectory.mkdirs();
        backupDirectory2.mkdirs();
        backupRepoDir = new File(backupArea, "backupRepo");
        backupRepoDir.mkdirs();
        new File(backupArea, "restoreRepo").mkdirs();
        FileUtil.delete("target/startup_test_indexes");
    }

    @After
    public void afterEach() throws Exception {
        testRepository.setDropOnExit(true);
        testRepository.shutdown();
        this.cleanDatabase();
    }

    @Test
    public void shouldKeepPersistentDataAcrossRestart() throws Exception {
        startRunStop(repository -> {
            Session session = repository.login();
            session.getRootNode().addNode("testNode");
            session.save();

            // create 2 new workspaces
            session.getWorkspace().createWorkspace("ws1");
            session.getWorkspace().createWorkspace("ws2");
            session.logout();
        }, true, false);

        startRunStop(repository -> {
            Session session = repository.login();
            assertNotNull(session.getNode("/testNode"));
            session.logout();

            // check the workspaces were persisted
            Session newWsSession = repository.login("ws1");
            newWsSession.getRootNode().addNode("newWsTestNode");
            newWsSession.save();
            newWsSession.logout();

            Session newWs1Session = repository.login("ws2");
            newWs1Session.getWorkspace().deleteWorkspace("ws2");
            newWs1Session.logout();
        }, false, false);

        startRunStop(repository -> {
            Session newWsSession = repository.login("ws1");
            assertNotNull(newWsSession.getNode("/newWsTestNode"));
            newWsSession.logout();
            // check a workspace was deleted
            try {
                repository.login("ws2");
                fail("Workspace was not deleted from the repository");
            } catch (NoSuchWorkspaceException e) {
                // expected
            }
        }, false, true);
    }

    @Test
    public void shouldNotImportInitialContentIfWorkspaceContentsChanged() throws Exception {
        startRunStop(repository -> {
            Session ws1Session = repository.login();
            // check the initial import
            Node node = ws1Session.getNode("/a");
            assertNotNull(node);
            // remove the node initially imported and add a new one
            node.remove();
            ws1Session.getRootNode().addNode("testNode");

            ws1Session.save();
        }, true, false);

        startRunStop(repository -> {
            Session ws1Session = repository.login();
            try {
                ws1Session.getNode("/a");
                fail("The initial content should be be re-imported if a workspace is not empty");
            } catch (PathNotFoundException e) {
                // expected
            }
            ws1Session.getNode("/testNode");
        }, false, true);
    }

    @Test
    public void shouldPersistExternalProjectionToFederatedNodeMappings() throws Exception {
        startRunStop(repository -> {
            Session session = repository.login();
            Node testRoot = session.getRootNode().addNode("testRoot");
            session.save();

            FederationManager federationManager = ((Workspace) session.getWorkspace()).getFederationManager();

            federationManager.createProjection("/testRoot", "mock-source", MockConnector.DOC1_LOCATION, "federated1");
            federationManager.createProjection("/testRoot", "mock-source", MockConnector.DOC2_LOCATION, null);
            Node doc1Federated = session.getNode("/testRoot/federated1");
            assertNotNull(doc1Federated);
            assertEquals(testRoot.getIdentifier(), doc1Federated.getParent().getIdentifier());
        }, true, false);

        startRunStop(repository -> {
            Session session = repository.login();
            Node testRoot = session.getNode("/testRoot");
            assertNotNull(testRoot);

            Node doc1Federated = session.getNode("/testRoot/federated1");
            
            assertNotNull(doc1Federated);
//            assertNotNull(doc1Federated.getParent());
//            assertEquals(testRoot.getIdentifier(), doc1Federated.getParent().getIdentifier());

            Node doc2Federated = session.getNode("/testRoot" + MockConnector.DOC2_LOCATION);
            assertNotNull(doc2Federated);
//            assertEquals(testRoot.getIdentifier(), doc2Federated.getParent().getIdentifier());
        }, false, true);
    }

    @Test
    public void shouldKeepPreconfiguredProjectionsAcrossRestart() throws Exception {
        RepositoryOperation checkPreconfiguredProjection = repository -> {
            Session session = repository.login();
            assertNotNull(session.getNode("/preconfiguredProjection"));
        };
        startRunStop(checkPreconfiguredProjection, true, false);
        startRunStop(checkPreconfiguredProjection, false, true);
    }

    @Test
    public void shouldCleanStoredProjectionsIfNodesAreDeleted() throws Exception {
        startRunStop(repository -> {
            Session session = repository.login();
            session.getRootNode().addNode("testRoot");
            session.save();

            FederationManager federationManager = ((Workspace) session.getWorkspace()).getFederationManager();
            federationManager.createProjection("/testRoot", "mock-source", MockConnector.DOC1_LOCATION, "federated1");
            federationManager.createProjection("/testRoot", "mock-source", MockConnector.DOC2_LOCATION, "federated2");
            Node projection = session.getNode("/testRoot/federated1");
            assertNotNull(projection);

            projection.remove();
            session.save();
        }, true, false);

        startRunStop(repository -> {
            Session session = repository.login();
            // check the 2nd projection
            assertNotNull(session.getNode("/testRoot/federated2"));
            try {
                session.getNode("/testRoot/federated1");
                fail("Projection has not been cleaned up");
            } catch (PathNotFoundException e) {
                // expected
            }
        }, false, true);
    }

    @Test
    public void shouldAppendJournalEntriesBetweenRestarts() throws Exception {
        final List<Integer> recordsOnStartup = new ArrayList<>(2);
        RepositoryOperation operation = repository -> {
            Session session = repository.login();
            session.getRootNode().addNode("node1");
            session.save();
            session.logout();
            Thread.sleep(100);
            int records = repository.runningState().journal().allRecords(false).size();
            assertTrue(records > 0);
            recordsOnStartup.add(records);
        };
        startRunStop(operation, true, false);
        startRunStop(operation, false, true);

        int countFirstTime = recordsOnStartup.get(0);
        int countSecondTime = recordsOnStartup.get(1);
        assertTrue(countSecondTime > countFirstTime);
    }

    @Test
    public void shouldAllowChangingNamespacePrefixesInSession() throws Exception {
        final String prefix = "admb";
        final String uri = "http://www.admb.be/modeshape/admb/1.0";
        final String nodeTypeName = prefix + ":test";

        startRunStop(repository -> {
            Session session = repository.login();

            NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();

            // First create a namespace for the nodeType which is going to be added
            session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);

            // Start creating a nodeTypeTemplate, keep it basic.
            NodeTypeTemplate nodeTypeTemplate = nodeTypeManager.createNodeTypeTemplate();
            nodeTypeTemplate.setName(nodeTypeName);
            nodeTypeManager.registerNodeType(nodeTypeTemplate, false);

            session.getRootNode().addNode("testNode", nodeTypeName);
            session.save();

            Node node = session.getNode("/testNode");
            assertEquals(nodeTypeName, node.getPrimaryNodeType().getName());
            session.setNamespacePrefix("newPrefix", uri);
            assertEquals("newPrefix:test", node.getPrimaryNodeType().getName());
        }, true, false);

        startRunStop(repository -> {
            Session session = repository.login();
            Node node = session.getNode("/testNode");
            assertEquals(nodeTypeName, node.getPrimaryNodeType().getName());
        }, false, true);
    }

    @Test
    public void reindexingLocalProviderShouldRemoveExistingDataFirst() throws Exception {
        startRunStop(repository -> {
            JcrSession session = repository.login();
            session.getRootNode().addNode("testRoot");
            session.save();
            Thread.sleep(100);
            String sql = "select [jcr:path] from [nt:unstructured] where [jcr:name] = 'testRoot'";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            ValidateQuery.validateQuery().rowCount(1).useIndex("nodesByName").validate(query, query.execute());
            session.logout();
        }, true, false);
        startRunStop(repository -> {
            JcrSession session = repository.login();
            // force a re-index of the entire workspace - this should clear the existing indexes first
            session.getWorkspace().reindex();
            // then force a reindex of a certain path 
            session.getWorkspace().reindex("/testRoot");
            //then check that still only 1 node is returned
            String sql = "select [jcr:path] from [nt:unstructured] where [jcr:name] = 'testRoot'";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            ValidateQuery.validateQuery().rowCount(1).useIndex("nodesByName").validate(query, query.execute());
        }, false, true);
    }

    @Test
    public void shouldDisableACLsIfAllPoliciesAreRemoved() throws Exception {
        startRunStop(repository -> {
            Session session = repository.login();
            Node testNode = session.getRootNode().addNode("testNode");
            testNode.addNode("node1");
            testNode.addNode("node2");
            session.save();

            AccessControlManager acm = session.getAccessControlManager();

            AccessControlList aclNode1 = getACL(acm, "/testNode/node1");
            aclNode1.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                    new Privilege[]{acm.privilegeFromName(Privilege.JCR_ALL)});
            acm.setPolicy("/testNode/node1", aclNode1);

            AccessControlList aclNode2 = getACL(acm, "/testNode/node2");
            aclNode2.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                    new Privilege[]{acm.privilegeFromName(Privilege.JCR_ALL)});
            acm.setPolicy("/testNode/node2", aclNode2);

            // access control should not be enabled yet because we haven't saved the session
            assertFalse(repository.runningState().repositoryCache().isAccessControlEnabled());

            session.save();
            assertTrue(repository.runningState().repositoryCache().isAccessControlEnabled());
        }, true, false);

        startRunStop(repository -> {
            assertTrue(repository.runningState().repositoryCache().isAccessControlEnabled());

            Session session = repository.login();
            AccessControlManager acm = session.getAccessControlManager();

            acm.removePolicy("/testNode/node1", null);
            acm.removePolicy("/testNode/node2", null);
            session.save();

            assertFalse(repository.runningState().repositoryCache().isAccessControlEnabled());

            session.getNode("/testNode").remove();
            session.save();
        }, false, true);
    }

    @Test
    public void shouldUseIndexesAfterRestarting() throws Exception {
        startRunStop(repository -> {
            JcrSession session = repository.login();
            session.getRootNode().addNode("testRoot");
            session.save();
            Thread.sleep(100);
            String sql = "select [jcr:path] from [nt:unstructured] where [jcr:name] = 'testRoot'";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            ValidateQuery.validateQuery().rowCount(1).useIndex("nodesByName").validate(query, query.execute());
            session.logout();
        }, true, false);

        startRunStop(repository -> {
            JcrSession session = repository.login();
            String sql = "select [jcr:path] from [nt:unstructured] where [jcr:name] = 'testRoot'";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            ValidateQuery.validateQuery().rowCount(1).useIndex("nodesByName").validate(query, query.execute());
            session.logout();
        }, false, true);
    }

    @Test
    public void shouldHaveVersionHistoryWhenRefreshIsCalled() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager vm = session.versionManager();
            Node outerNode = session.getRootNode().addNode("outerFolder");
            Node innerNode = outerNode.addNode("innerFolder");
            Node fileNode = innerNode.addNode("testFile.dat");
            fileNode.setProperty("jcr:mimeType", "text/plain");
            fileNode.setProperty("jcr:data", "Original content");
            session.save();

            assertFalse(hasVersionHistory(vm, fileNode));
            fileNode.addMixin("mix:versionable");
            // Version history is not created until save
            assertFalse(hasVersionHistory(vm, fileNode));
            session.refresh(true);
            // Version history is not created until save
            assertFalse(hasVersionHistory(vm, fileNode));
            session.save();
            assertTrue(hasVersionHistory(vm, fileNode));
        }, true, false);

        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager vm = session.versionManager();
            Node fileNode = session.getNode("/outerFolder/innerFolder/testFile.dat");
            assertTrue(hasVersionHistory(vm, fileNode));
        }, false, true);

    }

    @Test
    public void shouldAllowAddingUnderCheckedInNodeNewChildNodeWithOpvOfIgnore() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager vm = session.versionManager();
            // Set up parent node and check it in ...
            Node parent = session.getRootNode().addNode("versionableNode", "ver:versionable");
            parent.setProperty("versionProp", "v");
            parent.setProperty("copyProp", "c");
            parent.setProperty("ignoreProp", "i");
            session.save();
            vm.checkin(parent.getPath());

            // Try to add child with OPV of ignore ...
            Node child = parent.addNode("nonVersionedIgnoredChild", "ver:nonVersionableChild");
            child.setProperty("copyProp", "c");
            child.setProperty("ignoreProp", "i");
            session.save();

            // Try to update the properties on the child with OPV of 'ignore'
            child.setProperty("copyProp", "c2");
            child.setProperty("ignoreProp", "i2");
            session.save();

            // Try to add versionable child with OPV of ignore ...
            Node child2 = parent.addNode("versionedIgnoredChild", "ver:versionableChild");
            child2.setProperty("copyProp", "c");
            child2.setProperty("ignoreProp", "i");
            session.save();

            // Try to update the properties on the child with OPV of 'ignore'
            child2.setProperty("copyProp", "c2");
            child2.setProperty("ignoreProp", "i2");
            session.save();
        }, true, true);
    }

    @Test
    public void shouldNotAllowAddingUnderCheckedInNodeNewChildNodeWithOpvOfSomethingOtherThanIgnore() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager vm = session.versionManager();
            // Set up parent node and check it in ...
            Node parent = session.getRootNode().addNode("versionableNode", "ver:versionable");
            parent.setProperty("versionProp", "v");
            parent.setProperty("copyProp", "c");
            parent.setProperty("ignoreProp", "i");
            session.save();
            vm.checkin(parent.getPath());

            // Try to add versionable child with OPV of not ignore ...
            try {
                parent.addNode("versionedChild", "ver:versionableChild");
                fail("should have failed");
            } catch (VersionException e) {
                // expected
            }

            // Try to add non-versionable child with OPV of not ignore ...
            try {
                parent.addNode("nonVersionedChild", "ver:nonVersionableChild");
                fail("should have failed");
            } catch (VersionException e) {
                // expected
            }
        }, true, true);
    }

    @Test
    public void shouldAllowRemovingFromCheckedInNodeExistingChildNodeWithOpvOfIgnore() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager vm = session.versionManager();
            // Set up parent node and check it in ...
            Node parent = session.getRootNode().addNode("versionableNode", "ver:versionable");
            parent.setProperty("versionProp", "v");
            parent.setProperty("copyProp", "c");
            parent.setProperty("ignoreProp", "i");

            Node child1 = parent.addNode("nonVersionedIgnoredChild", "ver:nonVersionableChild");
            child1.setProperty("copyProp", "c");
            child1.setProperty("ignoreProp", "i");

            Node child2 = parent.addNode("versionedIgnoredChild", "ver:versionableChild");
            child2.setProperty("copyProp", "c");
            child2.setProperty("ignoreProp", "i");

            session.save();
            vm.checkin(parent.getPath());

            // Should be able to change the properties on the ignored children
            child1.setProperty("copyProp", "c2");
            child1.setProperty("ignoreProp", "i2");
            child2.setProperty("copyProp", "c2");
            child2.setProperty("ignoreProp", "i2");
            session.save();

            // Try to remove the two child nodes that have an OPV of 'ignore' ...
            child1.remove();
            child2.remove();
            session.save();

            // Should be able to change the ignored properties on the checked-in parent ...
            parent.setProperty("ignoreProp", "i");

            // Should not be able to set any non-ignored properties on the checked in parent ...
            try {
                parent.setProperty("copyProp", "c2");
                fail("not allowed");
            } catch (VersionException e) {
                // expected
            }
            try {
                parent.setProperty("versionProp", "v2");
                fail("not allowed");
            } catch (VersionException e) {
                // expected
            }
        }, true, true);
    }

    @Test
    public void shouldNotAllowRemovingFromCheckedInNodeExistingChildNodeWithOpvOfSomethingOtherThanIgnore() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager vm = session.versionManager();

            // Set up parent node and check it in ...
            Node parent = session.getRootNode().addNode("versionableNode", "ver:versionable");
            parent.setProperty("versionProp", "v");
            parent.setProperty("copyProp", "c");
            parent.setProperty("ignoreProp", "i");

            Node child1 = parent.addNode("nonVersionedChild", "ver:nonVersionableChild");
            child1.setProperty("copyProp", "c");
            child1.setProperty("ignoreProp", "i");

            Node child2 = parent.addNode("versionedChild", "ver:versionableChild");
            child2.setProperty("copyProp", "c");
            child2.setProperty("ignoreProp", "i");

            session.save();
            vm.checkin(parent.getPath());
            vm.checkin(child2.getPath());

            // Should not be able to set any non-ignored properties on the checked in parent ...
            try {
                parent.setProperty("copyProp", "c2");
                fail("not allowed");
            } catch (VersionException e) {
                // expected
            }
            try {
                parent.setProperty("versionProp", "v2");
                fail("not allowed");
            } catch (VersionException e) {
                // expected
            }

            // Should not be able to set any non-ignored properties on the non-ignored children ...
            try {
                child2.setProperty("copyProp", "c2");
                fail("not allowed");
            } catch (VersionException e) {
                // expected
            }
            try {
                child1.setProperty("copyProp", "c2");
                fail("not allowed");
            } catch (VersionException e) {
                // expected
            }

            // Check out the versionable child node, and we should be able to edit it ...
            vm.checkout(child2.getPath());
            child2.setProperty("copyProp", "c3");
            session.save();
            vm.checkin(child2.getPath());

            // But we still cannot edit a property on the nonVersionable child node when the parent is still checked in ...
            try {
                child1.setProperty("copyProp", "c2");
                fail("not allowed");
            } catch (VersionException e) {
                // expected
            }

            // Check out the parent ...
            vm.checkout(parent.getPath());

            // Now we can change the properties on the non-versionable children ...
            child1.setProperty("copyProp", "c2");
            session.save();

            // And even remove it ...
            child1.remove();
            session.save();

            // Check in the parent ...
            vm.checkin(parent.getPath());

            // and we cannot remove the child versionable node ...
            try {
                child2.remove();
                fail("not allowed");
            } catch (VersionException e) {
                // expected
            }

            // But once the parent is checked out ...
            vm.checkout(parent.getPath());

            // We can remove the versionable child that is checked in (!), since the parent is checked out ...
            // See Section 15.2.2:
            // "Note that remove of a read-only node is possible, as long as its parent is not read-only,
            // since removal is an alteration of the parent node."
            assertThat(vm.isCheckedOut(child2.getPath()), is(false));
            child2.remove();
            session.save();
        }, true, true);
    }

    @Test
    public void shouldAllowRemovingVersionFromVersionHistory() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();
            Node outerNode = session.getRootNode().addNode("outerFolder");
            Node innerNode = outerNode.addNode("innerFolder");
            Node fileNode = innerNode.addNode("testFile.dat");
            fileNode.setProperty("jcr:mimeType", "text/plain");
            fileNode.setProperty("jcr:data", "Original content");
            session.save();

            fileNode.addMixin("mix:versionable");
            session.save();

            // Make several changes ...
            String path = fileNode.getPath();
            for (int i = 2; i != 7; ++i) {
                versionManager.checkout(path);
                fileNode.setProperty("jcr:data", "Original content " + i);
                session.save();
                versionManager.checkin(path);
            }

            // Get the version history ...
            VersionHistory history = versionManager.getVersionHistory(path);
            assertThat(history, is(notNullValue()));
            assertThat(history.getAllLinearVersions().getSize(), is(6L));

            // Get the versions ...
            VersionIterator iter = history.getAllLinearVersions();
            Version v1 = iter.nextVersion();
            Version v2 = iter.nextVersion();
            Version v3 = iter.nextVersion();
            Version v4 = iter.nextVersion();
            Version v5 = iter.nextVersion();
            Version v6 = iter.nextVersion();
            assertThat(iter.hasNext(), is(false));
            String versionName = v3.getName();
            assertThat(v1, is(notNullValue()));
            assertThat(v2, is(notNullValue()));
            assertThat(v3, is(notNullValue()));
            assertThat(v4, is(notNullValue()));
            assertThat(v5, is(notNullValue()));
            assertThat(v6, is(notNullValue()));

            // Remove the 3rd version (that is, i=3) ...
            history.removeVersion(versionName);

            assertThat(history.getAllLinearVersions().getSize(), is(5L));

            // Get the versions using the history node we already have ...
            iter = history.getAllLinearVersions();
            Version v1a = iter.nextVersion();
            Version v2a = iter.nextVersion();
            Version v4a = iter.nextVersion();
            Version v5a = iter.nextVersion();
            Version v6a = iter.nextVersion();
            assertThat(iter.hasNext(), is(false));
            assertThat(v1a.getName(), is(v1.getName()));
            assertThat(v2a.getName(), is(v2.getName()));
            assertThat(v4a.getName(), is(v4.getName()));
            assertThat(v5a.getName(), is(v5.getName()));
            assertThat(v6a.getName(), is(v6.getName()));

            // Get the versions using a fresh history node ...
            VersionHistory history2 = versionManager.getVersionHistory(path);
            assertThat(history.getAllLinearVersions().getSize(), is(5L));

            iter = history2.getAllLinearVersions();
            Version v1b = iter.nextVersion();
            Version v2b = iter.nextVersion();
            Version v4b = iter.nextVersion();
            Version v5b = iter.nextVersion();
            Version v6b = iter.nextVersion();
            assertThat(iter.hasNext(), is(false));
            assertThat(v1b.getName(), is(v1.getName()));
            assertThat(v2b.getName(), is(v2.getName()));
            assertThat(v4b.getName(), is(v4.getName()));
            assertThat(v5b.getName(), is(v5.getName()));
            assertThat(v6b.getName(), is(v6.getName()));
        }, true, true);
    }

    @Test
    public void shouldAllowRemovingVersionFromVersionHistoryByRemovingVersionNode() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();
            Node outerNode = session.getRootNode().addNode("outerFolder");
            Node innerNode = outerNode.addNode("innerFolder");
            Node fileNode = innerNode.addNode("testFile.dat");
            fileNode.setProperty("jcr:mimeType", "text/plain");
            fileNode.setProperty("jcr:data", "Original content");
            session.save();

            fileNode.addMixin("mix:versionable");
            session.save();

            // Make several changes ...
            String path = fileNode.getPath();
            for (int i = 2; i != 7; ++i) {
                versionManager.checkout(path);
                fileNode.setProperty("jcr:data", "Original content " + i);
                session.save();
                versionManager.checkin(path);
            }

            // Get the version history ...
            VersionHistory history = versionManager.getVersionHistory(path);
            assertThat(history, is(notNullValue()));
            assertThat(history.getAllLinearVersions().getSize(), is(6L));

            // Get the versions ...
            VersionIterator iter = history.getAllLinearVersions();
            Version v1 = iter.nextVersion();
            Version v2 = iter.nextVersion();
            Version v3 = iter.nextVersion();
            Version v4 = iter.nextVersion();
            Version v5 = iter.nextVersion();
            Version v6 = iter.nextVersion();
            assertThat(iter.hasNext(), is(false));
            assertThat(v1, is(notNullValue()));
            assertThat(v2, is(notNullValue()));
            assertThat(v3, is(notNullValue()));
            assertThat(v4, is(notNullValue()));
            assertThat(v5, is(notNullValue()));
            assertThat(v6, is(notNullValue()));

            // Remove the 3rd version (that is, i=3) ...
            // history.removeVersion(versionName);
            try {
                v3.remove();
                fail("Should not allow removing a protected node");
            } catch (ConstraintViolationException e) {
                // expected
            }
        }, true, true);

    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldFindVersionNodeByIdentifierAndByUuid() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();
            // Set up parent node and check it in ...
            Node parent = session.getRootNode().addNode("versionableNode", "ver:versionable");
            parent.setProperty("versionProp", "v");
            parent.setProperty("copyProp", "c");
            parent.setProperty("ignoreProp", "i");
            session.save();
            Version version = versionManager.checkin(parent.getPath());

            // Now look for the version node by identifier, using the different ways to get an identifier ...
            assertThat(session.getNodeByIdentifier(version.getIdentifier()), is((Node) version));
            assertThat(session.getNodeByIdentifier(version.getProperty("jcr:uuid").getString()), is((Node) version));
            assertThat(session.getNodeByUUID(version.getProperty("jcr:uuid").getString()), is((Node) version));
        }, true, true);
    }

    @Test
    public void shouldMergeWorkspaces() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();

            session.getWorkspace().createWorkspace("original");
            session = repository.login("original");

            Node root = session.getRootNode();
            Node parent1 = root.addNode("parent1", "Parent");
            session.save();

            Node child1 = parent1.addNode("child1", "Child");
            assertThat(child1, is(notNullValue()));
            session.save();

            session.getWorkspace().createWorkspace("clone", "original");
            session = repository.login("clone");

            Node child2 = session.getNode("/parent1").addNode("child2", "Child");
            assertThat(child2, is(notNullValue()));
            session.save();

            session = repository.login("original");
            VersionManager vm = session.getWorkspace().getVersionManager();

            NodeIterator ni = vm.merge("/", "clone", true);
            session.save();

            session.getNode("/parent1/child2");
        }, true, true);
    }

    @Test
    public void shouldRemoveMixVersionablePropertiesWhenRemovingMixin() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            Node node = session.getRootNode().addNode("testNode");
            node.addMixin(JcrMixLexicon.VERSIONABLE.getString());
            session.save();

            // mix:referenceable
            assertNotNull(node.getProperty(JcrLexicon.UUID.getString()));
            // mix:simpleVersionable
            assertTrue(node.getProperty(JcrLexicon.IS_CHECKED_OUT.getString()).getBoolean());
            // mix:versionable
            assertNotNull(node.getProperty(JcrLexicon.BASE_VERSION.getString()));
            assertNotNull(node.getProperty(JcrLexicon.VERSION_HISTORY.getString()));
            assertNotNull(node.getProperty(JcrLexicon.PREDECESSORS.getString()));

            node.removeMixin(JcrMixLexicon.VERSIONABLE.getString());
            session.save();

            // mix:referenceable
            assertPropertyIsAbsent(node, JcrLexicon.UUID.getString());
            // mix:simpleVersionable
            assertPropertyIsAbsent(node, JcrLexicon.IS_CHECKED_OUT.getString());
            // mix:versionable
            assertPropertyIsAbsent(node, JcrLexicon.VERSION_HISTORY.getString());
            assertPropertyIsAbsent(node, JcrLexicon.BASE_VERSION.getString());
            assertPropertyIsAbsent(node, JcrLexicon.PREDECESSORS.getString());
        }, true, true);
    }

    @Test
    public void shouldRelinkVersionablePropertiesWhenRemovingAndReaddingMixVersionable() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();
            JcrVersionManager jcrVersionManager = (JcrVersionManager) versionManager;

            Node node = session.getRootNode().addNode("testNode");
            node.addMixin(JcrMixLexicon.VERSIONABLE.getString());
            session.save();
            // create a new version
            jcrVersionManager.checkin("/testNode");
            jcrVersionManager.checkout("/testNode");
            jcrVersionManager.checkin("/testNode");

            JcrVersionHistoryNode originalVersionHistory = jcrVersionManager.getVersionHistory("/testNode");
            Version originalBaseVersion = jcrVersionManager.getBaseVersion("/testNode");

            // remove the mixin
            jcrVersionManager.checkout("/testNode");
            node.removeMixin(JcrMixLexicon.VERSIONABLE.getString());
            session.save();

            // re-create the mixin and check the previous version history & versionable properties have been relinked.
            node.addMixin(JcrMixLexicon.VERSIONABLE.getString());
            session.save();

            // mix:referenceable
            assertNotNull(node.getProperty(JcrLexicon.UUID.getString()));
            // mix:simpleVersionable
            assertTrue(node.getProperty(JcrLexicon.IS_CHECKED_OUT.getString()).getBoolean());
            // mix:versionable
            assertNotNull(node.getProperty(JcrLexicon.BASE_VERSION.getString()));
            assertNotNull(node.getProperty(JcrLexicon.VERSION_HISTORY.getString()));
            assertNotNull(node.getProperty(JcrLexicon.PREDECESSORS.getString()));

            JcrVersionHistoryNode versionHistory = jcrVersionManager.getVersionHistory("/testNode");
            Version baseVersion = jcrVersionManager.getBaseVersion("/testNode");

            // check the actual
            assertEquals(originalVersionHistory.key(), versionHistory.key());
            assertEquals(originalBaseVersion.getCreated(), baseVersion.getCreated());
            assertEquals(originalBaseVersion.getPath(), baseVersion.getPath());
        }, true, true);
    }

    @Test
    public void shouldMergeEventualSuccessorVersions() throws Exception {
        // Create a "/record", make it versionable and check it in
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();
            session.getRootNode().addNode("record").addMixin("mix:versionable");
            session.save();
            versionManager.checkin("/record");

            // Clone QA version of data
            session.getWorkspace().createWorkspace("QA", session.getWorkspace().getName());
            Session sessionQa = repository.login("QA");
            VersionManager versionManagerQa = sessionQa.getWorkspace().getVersionManager();

            // Change QA node first time
            versionManagerQa.checkout("/record");
            sessionQa.getNode("/record").setProperty("111", "111");
            sessionQa.save();
            versionManagerQa.checkin("/record");

            // Change QA node second time
            versionManagerQa.checkout("/record");
            sessionQa.getNode("/record").setProperty("222", "222");
            sessionQa.save();
            versionManagerQa.checkin("/record");

            // Checks before merge
            // Check basic node - should not have any properties
            assertFalse(session.getNode("/record").hasProperty("111"));
            assertFalse(session.getNode("/record").hasProperty("222"));
            // Check QA node - should have properties 111=111 and 222=222
            assertTrue(sessionQa.getNode("/record").hasProperty("111"));
            assertTrue(sessionQa.getNode("/record").hasProperty("222"));
            assertEquals("111", sessionQa.getNode("/record").getProperty("111").getString());
            assertEquals("222", sessionQa.getNode("/record").getProperty("222").getString());

            // Merge
            versionManager.merge("/record", sessionQa.getWorkspace().getName(), true);

            // Checks after merge - basic node should have properties 111=111 and 222=222
            assertTrue(session.getNode("/record").hasProperty("111"));
            assertTrue(session.getNode("/record").hasProperty("222"));
            assertEquals("111", session.getNode("/record").getProperty("111").getString());
            assertEquals("222", session.getNode("/record").getProperty("222").getString());

            sessionQa.logout();
        }, true, true);
    }

    @Test
    public void shouldSetMergeFailedPropertyIfNodeIsCheckedIn() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();
            // Create a record, make it versionable and check it in
            session.getRootNode().addNode("record").addMixin("mix:versionable");
            session.save();
            versionManager.checkin("/record");

            // Clone QA version of data
            session.getWorkspace().createWorkspace("QA", session.getWorkspace().getName());
            Session sessionQa = repository.login("QA");
            try {
                VersionManager versionManagerQa = sessionQa.getWorkspace().getVersionManager();

                // Change QA node first time
                versionManagerQa.checkout("/record");
                sessionQa.getNode("/record").setProperty("111", "111");
                sessionQa.save();
                versionManagerQa.checkin("/record");

                // Change QA node second time, store version
                versionManagerQa.checkout("/record");
                sessionQa.getNode("/record").setProperty("222", "222");
                sessionQa.save();
                Version offendingVersion = versionManagerQa.checkin("/record");

                // Change original node one time to make versions in this workspace and other workspace be on
                // divergent branches, causing merge() to fail
                versionManager.checkout("/record");
                session.getNode("/record").setProperty("333", "333");
                session.save();
                versionManager.checkin("/record");

                // Try to merge
                NodeIterator nodeIterator = versionManager.merge("/record", sessionQa.getWorkspace().getName(), true);
                assertTrue(nodeIterator.hasNext());

                while (nodeIterator.hasNext()) {
                    Node record = nodeIterator.nextNode();
                    Version mergeFailedVersion = (Version) session.getNodeByIdentifier(record.getProperty("jcr:mergeFailed")
                            .getValues()[0].getString());
                    assertEquals(offendingVersion.getIdentifier(), mergeFailedVersion.getIdentifier());
                    versionManager.cancelMerge("/record", mergeFailedVersion);
                    assertFalse(record.hasProperty("jcr:mergeFailed"));
                }
            } finally {
                sessionQa.logout();
            }
        }, true, true);
    }

    @Test
    public void shouldSetMergeFailedPropertyIfNodeIsCheckedIn2() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();
            // Create a record, make it versionable and check it in
            session.getRootNode().addNode("record").addMixin("mix:versionable");
            session.save();
            versionManager.checkin("/record");

            // Clone QA version of data
            session.getWorkspace().createWorkspace("QA", session.getWorkspace().getName());
            Session sessionQa = repository.login("QA");
            try {
                VersionManager versionManagerQa = sessionQa.getWorkspace().getVersionManager();

                // Change QA node first time, store version
                versionManagerQa.checkout("/record");
                sessionQa.getNode("/record").setProperty("111", "111");
                sessionQa.save();
                Version offendingVersion1 = versionManagerQa.checkin("/record");

                // Change original node one time to make versions in this workspace and other workspace be on
                // divergent branches, causing merge() to fail
                versionManager.checkout("/record");
                session.getNode("/record").setProperty("333", "333");
                session.save();
                versionManager.checkin("/record");

                // Try to merge with offendingVersion1
                // This should create a new jcr:mergeFailed property
                versionManager.merge("/record", sessionQa.getWorkspace().getName(), true);

                // Change QA node second time, store version
                versionManagerQa.checkout("/record");
                sessionQa.getNode("/record").setProperty("222", "222");
                sessionQa.save();
                Version offendingVersion2 = versionManagerQa.checkin("/record");

                // Try to merge with offendingVersion2
                // This should add to existing jcr:mergeFailed property
                NodeIterator nodeIterator = versionManager.merge("/record", sessionQa.getWorkspace().getName(), true);

                assertTrue(nodeIterator.hasNext());
                while (nodeIterator.hasNext()) {
                    Node record = nodeIterator.nextNode();
                    Version mergeFailedVersion1 = (Version) session.getNodeByIdentifier(record.getProperty("jcr:mergeFailed")
                            .getValues()[0].getString());
                    assertEquals(offendingVersion1.getIdentifier(), mergeFailedVersion1.getIdentifier());
                    Version mergeFailedVersion2 = (Version) session.getNodeByIdentifier(record.getProperty("jcr:mergeFailed")
                            .getValues()[1].getString());
                    assertEquals(offendingVersion2.getIdentifier(), mergeFailedVersion2.getIdentifier());
                    versionManager.cancelMerge("/record", mergeFailedVersion1);
                    versionManager.cancelMerge("/record", mergeFailedVersion2);
                    assertFalse(record.hasProperty("jcr:mergeFailed"));
                }
            } finally {
                sessionQa.logout();
            }
        }, true, true);
    }

    @Test
    public void shouldMergeNodesWithSameNamesById() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();
            // Create a parent and two children, make them versionable and check them in
            Node parent = session.getRootNode().addNode("parent");
            parent.addMixin("mix:versionable");
            Node child1 = parent.addNode("child");
            child1.addMixin("mix:versionable");
            child1.setProperty("myproperty", "CHANGEME");
            Node child2 = parent.addNode("child");
            child2.addMixin("mix:versionable");
            child2.setProperty("myproperty", "222");
            session.save();
            versionManager.checkin(parent.getPath());
            versionManager.checkin(child1.getPath());
            versionManager.checkin(child2.getPath());

            // Clone QA version of data
            session.getWorkspace().createWorkspace("QA", session.getWorkspace().getName());
            Session sessionQa = repository.login("QA");
            VersionManager versionManagerQa = sessionQa.getWorkspace().getVersionManager();

            try {
                // QA: change child1's property
                Node qaParent = sessionQa.getNode("/parent");
                versionManagerQa.checkout(qaParent.getPath());
                Node qaChild1 = sessionQa.getNodeByIdentifier(child1.getIdentifier());
                versionManagerQa.checkout(qaChild1.getPath());
                qaChild1.setProperty("myproperty", "111");

                // QA: Add three new children with same name/path to parent
                Node qaChild3 = qaParent.addNode("child");
                qaChild3.addMixin("mix:versionable");
                qaChild3.setProperty("myproperty", "333");

                Node qaChild4 = qaParent.addNode("child");
                qaChild4.addMixin("mix:versionable");
                qaChild4.setProperty("myproperty", "444");

                Node qaChild5 = qaParent.addNode("child");
                qaChild5.addMixin("mix:versionable");
                qaChild5.setProperty("myproperty", "555");

                // QA: drop child2
                Node qaChild2 = sessionQa.getNodeByIdentifier(child2.getIdentifier());
                qaChild2.remove();

                sessionQa.save();
                Version qaChild1Version = versionManagerQa.checkin(qaChild1.getPath());
                Version qaChild3Version = versionManagerQa.checkin(qaChild3.getPath());
                Version qaChild4Version = versionManagerQa.checkin(qaChild4.getPath());
                Version qaChild5Version = versionManagerQa.checkin(qaChild5.getPath());
                Version qaParentVersion = versionManagerQa.checkin(qaParent.getPath());

                // Merge
                NodeIterator nodeIterator = versionManager.merge("/parent", sessionQa.getWorkspace().getName(), true);

                parent = session.getNodeByIdentifier(parent.getIdentifier());
                child1 = session.getNodeByIdentifier(qaChild1.getIdentifier());
                try {
                    session.getNodeByIdentifier(child2.getIdentifier()); // this one got removed
                    fail("Deleted child should not be retrieved");
                } catch (ItemNotFoundException e) {
                    //continue
                }
                Node child3 = session.getNodeByIdentifier(qaChild3.getIdentifier());
                Node child4 = session.getNodeByIdentifier(qaChild4.getIdentifier());
                Node child5 = session.getNodeByIdentifier(qaChild5.getIdentifier());

                // Run some checks using default workspace's versionManager and session
                assertFalse(nodeIterator.hasNext());

                assertEquals(qaParentVersion.getIdentifier(), versionManager.getBaseVersion(qaParent.getPath()).getIdentifier());
                assertEquals(qaChild1Version.getIdentifier(), versionManager.getBaseVersion(child1.getPath()).getIdentifier());
                assertEquals(qaChild3Version.getIdentifier(), versionManager.getBaseVersion(child3.getPath()).getIdentifier());
                assertEquals(qaChild4Version.getIdentifier(), versionManager.getBaseVersion(child4.getPath()).getIdentifier());
                assertEquals(qaChild5Version.getIdentifier(), versionManager.getBaseVersion(child5.getPath()).getIdentifier());

                // Check that parent no longer has child2
                for (NodeIterator childIterator = parent.getNodes(); childIterator.hasNext();) {
                    Node child = childIterator.nextNode();
                    assertFalse(child.getIdentifier().equals(child2.getIdentifier()));
                }
                assertEquals(1, child1.getIndex());
                assertEquals(2, child3.getIndex());
                assertEquals(3, child4.getIndex());
                assertEquals(4, child5.getIndex());

                assertEquals("111", child1.getProperty("myproperty").getString());
                assertEquals("333", child3.getProperty("myproperty").getString());
                assertEquals("444", child4.getProperty("myproperty").getString());
                assertEquals("555", child5.getProperty("myproperty").getString());
            } finally {
                sessionQa.logout();
            }
        }, true, true);
    }

    @Test
    public void shouldRestoreNodeWithVersionedChildrenUsingCheckpoints() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();
            // Create a parent and two children, make them versionable and check them in
            Node parent = session.getRootNode().addNode("parent");
            parent.addMixin("mix:versionable");
            Node child1 = parent.addNode("child1");
            child1.addMixin("mix:versionable");
            child1.setProperty("myproperty", "v1_1");
            Node child2 = parent.addNode("child2");
            child2.addMixin("mix:versionable");
            child2.setProperty("myproperty", "v2_1");
            session.save();

            Version v1 = versionManager.checkpoint(parent.getPath());
            assertEquals("1.0", v1.getName());

            child1.setProperty("myproperty", "v1_2");
            child2.setProperty("myproperty", "v2_2");
            session.save();
            Version v2 = versionManager.checkpoint(parent.getPath());

            assertEquals("1.1", v2.getName());

            versionManager.restore(parent.getPath(), "1.0", true);
            parent = session.getNode("/parent");
            assertEquals("v1_2", parent.getNode("child1").getProperty("myproperty").getString());
            assertEquals("v2_2", parent.getNode("child2").getProperty("myproperty").getString());
        }, true, true);
    }

    @Test
    public void shouldRestoreNodeWithoutVersionedChildrenUsingCheckpoints() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();

            Node node = session.getRootNode().addNode("revert", "jj:page");
            node.addNode("child1", "jj:content");
            node.addNode("child2", "jj:content");
            session.save();
            //create two versions
            versionManager.checkpoint(node.getPath());
            versionManager.checkpoint(node.getPath());
            versionManager.restore(node.getPath(), "1.0", true);
        }, true, true);
    }

    @Test
    public void shouldNotReturnTheVersionHistoryNode() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();
            Node node = session.getRootNode().addNode("outerFolder");
            node.setProperty("jcr:mimeType", "text/plain");
            node.addMixin("mix:versionable");
            session.save();
            versionManager.checkpoint("/outerFolder");

            node.remove();
            session.save();

            try {
                session.getNodeByIdentifier(node.getIdentifier());
                fail("Removed versionable node should not be retrieved ");
            } catch (ItemNotFoundException e) {
                //expected
            }
        }, true, true);
    }

    @Test
    public void shouldKeepOrderWhenRestoring() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();

            Node parent = session.getRootNode().addNode("parent", "jj:page");
            parent.addNode("child1", "jj:content");
            parent.addNode("child2", "jj:content");
            parent.addNode("child3", "jj:content");
            parent.addNode("child4", "jj:content");
            session.save();
            versionManager.checkpoint(parent.getPath());

            parent = session.getNode("/parent");
            parent.orderBefore("child4", "child3");
            parent.orderBefore("child3", "child2");
            parent.orderBefore("child2", "child1");
            session.save();

            Version version = versionManager.getBaseVersion(parent.getPath());
            versionManager.restore(version, true);

            parent = session.getNode("/parent");
            List<String> children = new ArrayList<String>();
            NodeIterator nodeIterator = parent.getNodes();
            while (nodeIterator.hasNext()) {
                children.add(nodeIterator.nextNode().getPath());
            }
            assertEquals(Arrays.asList("/parent/child1", "/parent/child2", "/parent/child3", "/parent/child4"), children);
        }, true, true);
    }

    @Test
    public void shouldRestoreAfterRemovingAndReaddingNodesWithSameName() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();

            Node parent = session.getRootNode().addNode("parent", "jj:page");
            Node child = parent.addNode("child", "jj:content");
            child.addNode("descendant", "jj:content");
            child.addNode("descendant", "jj:content");
            session.save();
            versionManager.checkpoint(parent.getPath());

            child = session.getNode("/parent/child");
            NodeIterator childNodes = child.getNodes();
            while (childNodes.hasNext()) {
                childNodes.nextNode().remove();
            }
            child.addNode("descendant", "jj:content");
            child.addNode("descendant", "jj:content");
            session.save();

            Version version = versionManager.getBaseVersion(parent.getPath());
            versionManager.restore(version, true);

            parent = session.getNode("/parent");
            assertEquals(Arrays.asList("/parent/child", "/parent/child/descendant", "/parent/child/descendant[2]"),
                    allChildrenPaths(parent));
        }, true, true);
    }

    @Test
    public void shouldRestoreMovedNode() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();

            Node parent = session.getRootNode().addNode("parent", "jj:page");
            parent.addNode("child", "jj:content");
            session.save();

            versionManager.checkpoint(parent.getPath());

            parent.addNode("_context", "jj:context");
            // move to a location under a new parent
            session.move("/parent/child", "/parent/_context/child");
            session.save();

            // restore
            versionManager.restore(parent.getPath(), "1.0", true);
            //the default OPV is COPY, so we expect the restore to have removed _context
            assertNoNode(session, "/parent/_context");
            assertNode(session, "parent/child");
        }, true, true);
    }

    @Test
    public void shouldRestoreToMultipleVersionsWhenEachVersionHasDifferentChild() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();

            // Add a page node with one child then make a version 1.0
            Node node = session.getRootNode().addNode("page", "jj:page");
            node.addNode("child1", "jj:content");
            session.save();
            versionManager.checkpoint(node.getPath());

            // add second child then make version 1.1
            node.addNode("child2", "jj:content");
            session.save();
            versionManager.checkpoint(node.getPath());
            // restore to 1.0
            versionManager.restore(node.getPath(), "1.0", true);
            assertNode(session, "page/child1");
            assertNoNode(session, "/page/child2");
            // then restore to 1.1, it will throw the NullPointException
            versionManager.restore(node.getPath(), "1.1", true);
            assertNode(session, "page/child1");
            assertNode(session, "page/child2");
        }, true, true);
    }

    @Test
    public void shouldRestoreMovedNode2() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();

            Node parent = session.getRootNode().addNode("parent", "jj:page");
            parent.addNode("_context", "jj:context");
            parent.addNode("child", "jj:content");
            session.save();

            versionManager.checkpoint(parent.getPath());

            // move to a location under a new parent
            session.move("/parent/child", "/parent/_context/child");
            session.save();

            // restore
            versionManager.restore(parent.getPath(), "1.0", true);
            assertNoNode(session, "/parent/_context/child");
            assertNode(session, "parent/child");
        }, true, true);
    }

    @Test
    public void shouldRestoreMovedNode3() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();

            Node parent = session.getRootNode().addNode("parent", "jj:page");
            parent.addNode("child1", "jj:content");
            parent.addNode("_context", "jj:context");
            parent.addNode("child2", "jj:content");
            session.save();

            versionManager.checkpoint(parent.getPath());

            // move to a location under a new parent
            session.move("/parent/child1", "/parent/_context/child1");
            session.save();

            // restore
            versionManager.restore(parent.getPath(), "1.0", true);
            assertNoNode(session, "/parent/_context/child1");
            assertNoNode(session, "/parent/_context/child2");
            assertNode(session, "parent/child1");
            assertNode(session, "parent/child2");
            assertNode(session, "parent/_context");
        }, true, true);
    }

    @Test
    public void shouldRemoveVersionInVersionGraphWithBranches1() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();
            Node node = session.getRootNode().addNode("node", "jj:page");
            session.save();

            Version v1 = versionManager.checkpoint(node.getPath());
            assertEquals("1.0", v1.getName());
            assertEquals(2, versionManager.getVersionHistory("/node").getAllVersions().getSize());

            Version v2 = versionManager.checkin("/node");
            assertEquals("1.1", v2.getName());
            assertEquals(3, versionManager.getVersionHistory("/node").getAllVersions().getSize());

            versionManager.restore(v1, true);
            assertEquals(3, versionManager.getVersionHistory("/node").getAllVersions().getSize());

            Version baseVersion = versionManager.checkpoint(node.getPath());
            assertEquals("1.0", baseVersion.getName());
            assertEquals(3, versionManager.getVersionHistory("/node").getAllVersions().getSize());

            Version v4 = versionManager.checkin("/node");
            assertEquals("1.1.0", v4.getName());
            assertEquals(4, versionManager.getVersionHistory("/node").getAllVersions().getSize());

            versionManager.getVersionHistory("/node").removeVersion(v1.getName());
        }, true, true);
    }

    @Test
    public void shouldRemoveVersionInVersionGraphWithBranches2() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();

            Node node = session.getRootNode().addNode("node", "jj:page");
            session.save();
            String nodePath = node.getPath();

            //create two versions
            versionManager.checkpoint(nodePath);    // version 1.0
            versionManager.checkpoint(nodePath);    // version 1.1
            versionManager.restore(nodePath, "1.0", true);
            versionManager.checkout(nodePath);
            versionManager.checkpoint(nodePath);    // version 1.1.0

            versionManager.getVersionHistory(nodePath).removeVersion("1.1");
        }, true, true);
    }

    @Test
    public void shouldRemoveEmptyVersionHistories() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();

            Node node1 = session.getRootNode().addNode("node1", "jj:page");
            node1.addNode("node11", "jj:page");
            node1.addNode("node12", "jj:content");
            session.save();

            ((org.modeshape.jcr.api.version.VersionManager) versionManager).remove("/node1");
            try {
                versionManager.getVersionHistory("/node1");
                fail("Version history has not been removed");
            } catch (PathNotFoundException e) {
                //expected
            }

            try {
                versionManager.getVersionHistory("/node1/node11");
                fail("Version history has not been removed");
            } catch (PathNotFoundException e) {
                //expected
            }
        }, true, true);
    }

    @Test
    public void shouldNotRemoveNonEmptyVersionHistories() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.versionManager();

            Node node1 = session.getRootNode().addNode("node1", "jj:page");
            node1.addNode("node11", "jj:page");
            session.save();

            versionManager.checkpoint("/node1/node11");
            try {
                ((org.modeshape.jcr.api.version.VersionManager) versionManager).remove("/node1");
                fail("Should not allow removal of non empty version history");
            } catch (UnsupportedRepositoryOperationException e) {
                //expected
            }
        }, true, true);
    }

    @Test
    public void shouldCreateSeparateVersionHistoryForCopiedNode() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.getWorkspace().getVersionManager();
            // create original node  
            Node node = session.getNode("/").addNode("uploads");
            Node originalNode = node.addNode("originalNode", NodeType.NT_FILE);
            originalNode.addMixin(NodeType.MIX_VERSIONABLE);
            originalNode.addMixin(NodeType.MIX_TITLE);
            Node contentNode = originalNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
            contentNode.setProperty(JcrConstants.JCR_DATA,
                    session.getValueFactory().createBinary(new ByteArrayInputStream("test".getBytes())));
            session.save();
            // update original node  
            versionManager.checkout(originalNode.getPath());
            originalNode.setProperty(Property.JCR_TITLE, "originalNode");
            session.save();
            versionManager.checkin(originalNode.getPath());

            VersionHistory originalHistory = versionManager.getVersionHistory(originalNode.getPath());
            Version originalBaseVersion = versionManager.getBaseVersion(originalNode.getPath());

            // copy original node  
            session.getWorkspace().copy(originalNode.getPath(), "/uploads/copiedNode");

            // check that the copied node is checked out and has its own version history
            Node copiedNode = session.getNode("/uploads/copiedNode");
            assertTrue(versionManager.isCheckedOut(copiedNode.getPath()));
            VersionHistory copiedHistory = versionManager.getVersionHistory(copiedNode.getPath());
            assertNotEquals(originalHistory.getVersionableIdentifier(), copiedHistory.getVersionableIdentifier());
            Version copiedBaseVersion = versionManager.getBaseVersion(copiedNode.getPath());
            assertNotEquals(originalBaseVersion.getPath(), copiedBaseVersion.getPath());

            // delete all versions for the original node
            VersionIterator it = originalHistory.getAllVersions();
            while (it.hasNext()) {
                Version version = it.nextVersion();
                originalHistory.removeVersion(version.getName());
            }

            // delete all versions for the copied node
            it = copiedHistory.getAllVersions();
            while (it.hasNext()) {
                Version version = it.nextVersion();
                copiedHistory.removeVersion(version.getName());
            }

            // delete original node  
            originalNode.remove();
            session.save();

            // delete the copied node
            copiedNode.remove();
            session.save();
        }, true, true);
    }

    @Test
    public void shouldReadChildVersionHistoryProperty() throws Exception {
        startRunStop(repository -> {
            JcrSession session = (JcrSession) repository.login();
            VersionManager versionManager = session.getWorkspace().getVersionManager();
            // Create a versionable parent node and add a single version.
            Node parentNode = session.getRootNode().addNode("parent");
            parentNode.addMixin("mix:versionable");
            session.save();

            VersionManager vm = parentNode.getSession().getWorkspace().getVersionManager();
            vm.checkout(parentNode.getPath());
            vm.checkin(parentNode.getPath());
            vm.checkout(parentNode.getPath());

            // Create a versionable child node for a previously created parent and add a single version.
            Node childNode = parentNode.addNode("child");
            childNode.addMixin("mix:versionable");
            session.save();
            vm.checkout(childNode.getPath());
            vm.checkin(childNode.getPath());
            vm.checkin(parentNode.getPath());

            // Obtain the base version of the parent node.
            Version baseParentVersion = vm.getBaseVersion(parentNode.getPath());
            Node frozenNode = baseParentVersion.getFrozenNode();
            NodeIterator nodeIterator = frozenNode.getNodes();

            while (nodeIterator.hasNext()) {
                Node child = nodeIterator.nextNode();
                assertThat(child.getProperty("jcr:primaryType").getString(), is("nt:versionedChild"));
                Property property = child.getProperty("jcr:childVersionHistory");
                VersionHistory versionHistory = (VersionHistory) property.getNode();
                // Do something about obtained version history...
                assertNotNull(versionHistory);
            }
        }, true, true);
    }

    
    
/**--------------------------------------------------------------------------*/

//    @Test
    public void shouldBackupRepositoryWhichIncludesBinaryValuesCompressed() throws Exception {
        startRunStop(repository -> {
            JcrSession session = repository.login();
            loadBinaries(session);
        }, true, false);
        
        startRunStop(repository -> {
            JcrSession session = repository.login();
            session.getWorkspace().createWorkspace("ws2");
            session.getWorkspace().createWorkspace("ws3");
            
            session = repository.login("ws2");
            loadBinaries(session);
            
            
            session = repository.login("ws3");
            loadBinaries(session);
        }, false, false);
        
        startRunStop(repository -> {
            JcrSession session = repository.login();
            makeBackup(session, BackupOptions.DEFAULT);
        }, false, true);
        
        this.restoreBackup(true);
        
        startRunStop(repository -> {
            JcrSession session = repository.login();
            verifyBinaryContent(session);
            
            session = repository.login("ws2");
            verifyBinaryContent(session);
                        
            session = repository.login("ws3");
            verifyBinaryContent(session);            
        }, false, true);
        
    }

    @Test
    public void shouldBackupRepositoryWhichIncludesBinaryValuesUnCompressed() throws Exception {
        startRunStop(repository -> {
            JcrSession session = repository.login();
            loadBinaries(session);
        }, true, false);
        
        startRunStop(repository -> {
            JcrSession session = repository.login();
            session.getWorkspace().createWorkspace("ws2");
            session.getWorkspace().createWorkspace("ws3");
            
            session = repository.login("ws2");
            loadBinaries(session);
            
            
            session = repository.login("ws3");
            loadBinaries(session);
        }, false, false);
        
        startRunStop(repository -> {
            JcrSession session = repository.login();
            BackupOptions backupOptions = new BackupOptions() {
                @Override
                public boolean compress() {
                    return false;
                }
            };
            makeBackup(session, backupOptions);
        }, false, true);
        
        this.restoreBackup(true);
        
        startRunStop(repository -> {
            JcrSession session = repository.login();
            verifyBinaryContent(session);
            
            session = repository.login("ws2");
            verifyBinaryContent(session);
                        
            session = repository.login("ws3");
            verifyBinaryContent(session);            
        }, false, true);
        
    }
    
    @Test
    public void shouldPreserveBinariesFromRestoredBackup() throws Exception {
        startRunStop(repository -> {
            JcrSession session = repository.login();
            loadBinaries(session);
            makeBackup(session, BackupOptions.DEFAULT);
        }, true, false);

        //wipe the repository
        startRunStop(repository -> {
            JcrSession session = repository.login();
        }, true, true);
        
        this.restoreBackup(true);
        
        startRunStop(repository -> {
            JcrSession session = repository.login();
            makeBackup(session, BackupOptions.DEFAULT);
        }, false, false);

        //wipe the repository
        startRunStop(repository -> {
            JcrSession session = repository.login();
        }, true, true);

        this.restoreBackup(true);
        startRunStop(repository -> {
            JcrSession session = repository.login();
            verifyBinaryContent(session);
        }, false, true);
    }
    
    @Test
    public void shouldBackupRepositoryWithMultipleWorkspaces() throws Exception {
        startRunStop(repository -> {
            JcrSession session = repository.login();
            session.getWorkspace().createWorkspace("ws2");
            session.getWorkspace().createWorkspace("ws3");
            
            
            session = repository.login("ws2");
            importInitialContent("data/cars.xml", session);
            
            session = repository.login("ws3");
            importInitialContent("data/cars.xml", session);
            
            session = repository.login();
            makeBackup(session, BackupOptions.DEFAULT);
    
            // Make some changes that will not be in the backup ...
            session.getRootNode().addNode("node-not-in-backup");
            session.save();
            assertContentInWorkspace(session, "/node-not-in-backup");

            session = repository.login("ws2");
            assertContentInWorkspace(session);
            
            session = repository.login("ws3");
            assertContentInWorkspace(session);
        }, true, false);


        this.restoreBackup(true);

        startRunStop(repository -> {
            JcrSession session = repository.login();

            assertContentNotInWorkspace(session, "/node-not-in-backup");
            assertContentInWorkspace(session);
            
            session = repository.login("ws2");
            assertContentInWorkspace(session);
            
            session = repository.login("ws3");
            assertContentInWorkspace(session);
        }, false, true);
        
    }    
    
    @Test
    public void shouldBackupAndRestoreWithExistingUserTransaction() throws Exception {
        startRunStop(repository -> {
            JcrSession session = repository.login();
            
            TransactionManager txnMgr = repository.transactionManager();
            txnMgr.begin();
            
            makeBackup(session, BackupOptions.DEFAULT);            
            restoreBackup(false);
            
            txnMgr.rollback();
            assertContentInWorkspace(session);
        }, true, true);
    }
    
    @Test
    public void shouldRestoreBinaryReferencesWhenExcludedFromBackup() throws Exception {
        startRunStop(repository -> {
            JcrSession session = repository.login();
            loadBinaries(session);
            BackupOptions backupOptions = new BackupOptions() {
                @Override
                public boolean includeBinaries() {
                    return false;
                }
            };
            makeBackup(session, backupOptions);
        }, true, false);

        //wipe the repository
        startRunStop(repository -> {
            JcrSession session = repository.login();
        }, true, true);

        RestoreOptions restoreOptions = new RestoreOptions() {
            @Override
            public boolean includeBinaries() {
                return false;
            }
        };
        this.restoreBackup(restoreOptions);
        
        startRunStop(repository -> {
            JcrSession session = repository.login();
            verifyBinaryContent(session);
        }, false, true);
    }
    
    @Test
    public void shouldPersistGeneratedNamespacesAcrossRestart() throws Exception {
        startRunStop(repository -> {
            Session session = repository.login();

            final NamespaceRegistry namespaceRegistry = (NamespaceRegistry)session.getWorkspace().getNamespaceRegistry();

            namespaceRegistry.registerNamespace("info:a#");
            namespaceRegistry.registerNamespace("info:b#");
            namespaceRegistry.registerNamespace("info:c#");
            assertEquals("ns001", namespaceRegistry.getPrefix("info:a#"));
            assertEquals("ns002", namespaceRegistry.getPrefix("info:b#"));
            assertEquals("ns003", namespaceRegistry.getPrefix("info:c#"));

            final Node node = session.getRootNode().addNode("ns001:xyz", NodeType.NT_UNSTRUCTURED);
            node.setProperty("ns002:abc", "abc");
            node.setProperty("ns003:def", "def");

            session.save();
            session.logout();
        }, true, false);

        startRunStop(repository -> {
            Session session = repository.login();

            final NamespaceRegistry namespaceRegistry = (NamespaceRegistry)session.getWorkspace().getNamespaceRegistry();

            assertEquals("ns001", namespaceRegistry.getPrefix("info:a#"));
            assertEquals("ns002", namespaceRegistry.getPrefix("info:b#"));
            assertEquals("ns003", namespaceRegistry.getPrefix("info:c#"));
            session.save();
            session.logout();
        }, false, true);

    }

    @Test
    public void shouldPersistDataAcrossRestart() throws Exception {
        startRunStop(repository -> {
            JcrSession session = repository.login();
            loadBinaries(session);
            
            session.getWorkspace().createWorkspace("ws2");
            session.save();
            
            session = repository.login("ws2");
            importInitialContent("data/cars.xml", session);
            session.save();
        }, true, false);

        startRunStop(repository -> {
            JcrSession session = repository.login("ws2");
            assertContentInWorkspace(session);
            Node node = session.getNode("/a/b/Cars/Utility/Ford F-150");
            node.setProperty("car:year", "2009");
            session.save();
        }, false, false);

        startRunStop(repository -> {
            JcrSession session = repository.login("ws2");
            Node node = session.getNode("/a/b/Cars/Utility/Ford F-150");
            assertEquals("2009", node.getProperty("car:year").getString());
            
            node.remove();
            session.save();
        }, false, false);

        startRunStop(repository -> {
            JcrSession session = repository.login("ws2");
            assertContentNotInWorkspace(session, "/a/b/Cars/Utility/Ford F-150");
        }, false, false);

        startRunStop(repository -> {
            JcrSession session = repository.login();
            verifyBinaryContent(session);
        }, false, true);
        
    }    

    @Test
    public void shouldPersistBinaryDataAcrossRestart() throws Exception {
        startRunStop(repository -> {
            JcrSession session = repository.login();
            loadBinaries(session);
            session.save();
        }, true, false);

        startRunStop(repository -> {
            JcrSession session = repository.login();
            verifyBinaryContent(session);
        }, false, true);
        
    }    
    
    @Test
    public void shouldPersistSequence() throws Exception {
        startRunStop(repository -> {
            org.modeshape.jcr.api.Session session = repository.login();
            InputStream stream = getClass().getClassLoader().getResourceAsStream("log4j.properties");
            Node node = session.getRootNode().addNode("log4j.properties", "nt:file");
            Node content = node.addNode("jcr:content", "nt:resource");
            content.setProperty("jcr:data", session.getValueFactory().createBinary(stream));

            Node output = session.getRootNode().addNode("output");
            assertFalse(output.hasNode(TestSequencersHolder.DERIVED_NODE_NAME));

            session.sequence("Counting sequencer", content.getProperty("jcr:data"), output);
            assertTrue(output.hasNode(TestSequencersHolder.DERIVED_NODE_NAME));
            session.save();
        }, true, false);
        
        startRunStop(repository -> {
            Session session = repository.login();
            Node output = session.getNode("/output");
            assertTrue(output.hasNode(TestSequencersHolder.DERIVED_NODE_NAME));
        }, false, true);
    }
  
    @Test
    @FixFor( "MODE-2607" )
    public void shouldUpdateParentAndRemoveChildWithDifferentTransactions1() throws Exception {
        startRunStop(repository -> {
            final String parentPath = "/parent";
            final String childPath = "/parent/child";
    
            TransactionManager txManager = repository.transactionManager();
            //create parent and child node with some properties in a tx
    
            txManager.begin();
            JcrSession session = repository.login();
            Node parent = session.getRootNode().addNode("parent");
            parent.setProperty("foo", "parent");
            Node child = parent.addNode("child");
            child.setProperty("foo", "child");
            session.save();
            txManager.commit();
    
            //edit the parent and remove the child in a new tx
            txManager.begin();
            session = repository.login();
            parent = session.getNode(parentPath);
            parent.setProperty("foo", "bar2");
            session.save();
    
            child = session.getNode(childPath);
            child.remove();
            session.save();
            txManager.commit();
    
            //check that the editing worked in a new tx
            txManager.begin();
            parent = session.getNode(parentPath);
            assertEquals("bar2", parent.getProperty("foo").getString());
            assertNoNode(session, "/parent/child");
            txManager.commit();    
        }, true, true);
    }
    
    @Test
    @FixFor( "MODE-2610" )
    public void shouldUpdateParentAndRemoveChildWithDifferentTransactions2() throws Exception {
        startRunStop(repository -> {
            final String parentPath = "/parent";
            final String childPath = "/parent/child";
    
            JcrSession session = repository.login();
            TransactionManager txManager = repository.transactionManager();
            txManager.begin();
            Node parent = session.getRootNode().addNode("parent");
            parent.setProperty("foo", "parent");
            Node child = parent.addNode("child");
            child.setProperty("foo", "child");
            session.save();
            txManager.commit();
    
            txManager.begin();
            child = session.getNode(childPath);
            parent = session.getNode(parentPath);
            parent.setProperty("foo", "bar2");
            session.save();
            child.remove();
            session.save();
            txManager.commit();
    
            txManager.begin();
            parent = session.getNode(parentPath);
            assertEquals("bar2", parent.getProperty("foo").getString());
            assertNoNode(session, "/parent/child");
            session.logout();
            txManager.commit();
        }, true, true);
    }
    
    @FixFor( "MODE-2623" )
    @Test
    public void shouldAllowLockUnlockWithinTransaction() throws Exception {
        startRunStop(repository -> {
            JcrSession session = repository.login();
            TransactionManager txManager = repository.transactionManager();
            
            final String path = "/test";
            Node parent = session.getRootNode().addNode("test");
            parent.addMixin("mix:lockable");
            session.save();
    
            txManager.begin();
            LockManager lockMgr = session.getWorkspace().getLockManager();
            lockMgr.lock(path, true, true, Long.MAX_VALUE, session.getUserID());
            lockMgr.unlock(path);
            txManager.commit();
            
            assertFalse(session.getNode(path).isLocked());
        }, true, true);
    }
    
    protected void startRunStop(RepositoryOperation operation,
            boolean createOnStart, boolean dropOnExit) {
        JcrRepository repository = null;

        try {
            testRepository.setDropOnExit(dropOnExit);
            testRepository.setCreateOnStart(createOnStart);
            testRepository.start();
            repository = (JcrRepository) testRepository.repository();
            operation.execute(repository);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (repository != null) {
                testRepository.shutdown();
            }
        }
    }

    @FunctionalInterface
    protected interface RepositoryOperation {

        void execute(JcrRepository repository) throws Exception;
    }

    protected AccessControlList getACL(AccessControlManager acm,
            String absPath) throws Exception {
        AccessControlPolicyIterator it = acm.getApplicablePolicies(absPath);
        if (it.hasNext()) {
            return (AccessControlList) it.nextAccessControlPolicy();
        }
        return (AccessControlList) acm.getPolicies(absPath)[0];
    }

    private boolean hasVersionHistory(VersionManager versionManager, Node node) throws RepositoryException {
        try {
            VersionHistory history = versionManager.getVersionHistory(node.getPath());
            assertNotNull(history);
            return true;
        } catch (UnsupportedRepositoryOperationException e) {
            return false;
        }
    }

    private void assertPropertyIsAbsent(Node node,
            String propertyName) throws Exception {
        try {
            node.getProperty(propertyName);
            fail("Property: " + propertyName + " was expected to be missing on node:" + node);
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    private List<String> allChildrenPaths(Node root) throws Exception {
        List<String> paths = new ArrayList<String>();
        NodeIterator nodeIterator = root.getNodes();
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.nextNode();
            paths.add(child.getPath());
            paths.addAll(allChildrenPaths(child));
        }
        return paths;
    }

    protected void assertNoNode(JcrSession session, String path) throws RepositoryException {
        // Verify that the parent node does exist now ...
        assertThat("Did not expect to find '" + path + "'", session.getRootNode().hasNode(relativePath(path)), is(false));
        try {
            session.getNode(path);
            fail("Did not expect to find node at \"" + path + "\"");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    protected Node assertNode(JcrSession session, String path) throws RepositoryException {
        if (!session.getRootNode().hasNode(path)) {
            // We won't find the node, so print out the information ...
            Node parent = session.getRootNode();
            for (Path.Segment segment : path(session, path)) {
                if (!parent.hasNode(asString(session, segment))) {
                    System.out.println("Unable to find '" + path + "'; lowest node is '" + parent.getPath() + "'");
                    break;
                }
                parent = parent.getNode(asString(session, segment));
            }
        }

        Node node = session.getNode("/" + path);
        assertThat(node, is(notNullValue()));
        // Verify that the path can be found via navigating ...
        if (path.trim().length() == 0) {
            // This is the root path, so of course it exists ...
            assertThat(session.getRootNode(), is(notNullValue()));
        } else {

        }
        return node;
    }

    protected Path path(JcrSession session, String path) {
        return session.context().getValueFactories().getPathFactory().create(path);
    }

    protected String relativePath(String path) {
        return !path.startsWith("/") ? path : path.substring(1);
    }

    protected String asString(JcrSession session, Object value) {
        return session.context().getValueFactories().getStringFactory().create(value);
    }
    
    protected void loadBinaries(Session session) throws Exception {
        for (int i = 0; i < BINARY_RESOURCES.length; i++) {
            String name = "file_" + i;
            Node fileNode = session.getRootNode().addNode(name, "nt:file");
            Node content = fileNode.addNode("jcr:content", "nt:resource");

            InputStream stream = ModeshapePersistenceIT.class.getClassLoader().getResourceAsStream(BINARY_RESOURCES[i]);
            content.setProperty("jcr:data", session.getValueFactory().createBinary(stream));
        }
        session.save();
    }
    
    private void makeBackup(JcrSession session, BackupOptions options) throws RepositoryException {
        FileUtil.delete(backupDirectory.getPath());
        Problems problems = session.getWorkspace().getRepositoryManager().backupRepository(backupDirectory, options);
        assertNoProblems(problems);
    }
    
    private void restoreBackup(boolean shutdownRepository) throws Exception {
        testRepository.setCreateOnStart(true);
        testRepository.setDropOnExit(false);
        testRepository.start();
        try {
            JcrSession session = (JcrSession) testRepository.login();
            Problems problems = session.getWorkspace().getRepositoryManager().restoreRepository(backupDirectory);
            assertNoProblems(problems);
            session.logout();
        } finally {
            if (shutdownRepository) {
                testRepository.shutdown();
            }
        }
    }

    private void restoreBackup(RestoreOptions opts) throws Exception {
        testRepository.setCreateOnStart(true);
        testRepository.setDropOnExit(false);
        testRepository.start();
        try {
            JcrSession session = (JcrSession) testRepository.login();
            Problems problems = session.getWorkspace().getRepositoryManager().restoreRepository(backupDirectory, opts);
            assertNoProblems(problems);
            session.logout();
        } finally {
            testRepository.shutdown();
        }
    }
    
    protected void assertNoProblems( Problems problems ) {
        if (problems.hasProblems()) {
            System.out.println(problems);
        }
        assertThat(problems.hasProblems(), is(false));
    }

    private void verifyBinaryContent(JcrSession session) throws Exception {
        for (int i = 0; i < BINARY_RESOURCES.length; i++) {
            String fileName = "/file_" + i;
            Node file = session.getNode(fileName).getNode("jcr:content");
            Binary binary = file.getProperty("jcr:data").getBinary();
            assertNotNull(binary);

            InputStream in = getClass().getClassLoader().getResourceAsStream(BINARY_RESOURCES[i]);
            byte[] expectedContent = IoUtil.readBytes(in);
            byte[] actualContent = IoUtil.readBytes(binary.getStream());
            assertArrayEquals("Binary content to valid for " + fileName, expectedContent, actualContent);
        }
    }
    
    private void assertContentInWorkspace(JcrSession session, String... paths) throws RepositoryException {
        session.getRootNode();
        session.getNode("/a/b/Cars");
        session.getNode("/a/b/Cars/Hybrid");
        session.getNode("/a/b/Cars/Hybrid/Toyota Prius");
        session.getNode("/a/b/Cars/Hybrid/Toyota Highlander");
        session.getNode("/a/b/Cars/Hybrid/Nissan Altima");
        session.getNode("/a/b/Cars/Sports/Aston Martin DB9");
        session.getNode("/a/b/Cars/Sports/Infiniti G37");
        session.getNode("/a/b/Cars/Luxury/Cadillac DTS");
        session.getNode("/a/b/Cars/Luxury/Bentley Continental");
        session.getNode("/a/b/Cars/Luxury/Lexus IS350");
        session.getNode("/a/b/Cars/Utility/Land Rover LR2");
        session.getNode("/a/b/Cars/Utility/Land Rover LR3");
        session.getNode("/a/b/Cars/Utility/Hummer H3");
        session.getNode("/a/b/Cars/Utility/Ford F-150");
        for (String path : paths) {
            session.getNode(path);
        }
    }

    private void assertContentNotInWorkspace(JcrSession session, String... paths) throws RepositoryException {
        session.getRootNode();
        for (String path : paths) {
            try {
                session.getNode(path);
                fail("Should not have found '" + path + "'");
            } catch (PathNotFoundException e) {
                // expected
            }
        }
    }
    
    private void importInitialContent(String resourceName, Session session) throws RepositoryException, IOException {
        testRepository.loadInitialContent(resourceName, session);
    }
    
    private void cleanDatabase() {
        Connection conn = null;
        try {
            String driverName = System.getProperty("db.driver");
            String username = System.getProperty("db.username");
            String password = System.getProperty("db.password");
            String url = System.getProperty("db.url");
            
            Class.forName(driverName);
            conn = DriverManager.getConnection(url, username, password);
            Statement deleteQuery = conn.createStatement();
            deleteQuery.executeQuery("drop table MODESHAPE_REPOSITORY");
        } catch (ClassNotFoundException | SQLException e) {
        } finally {
            if (conn != null) 
                try {
                    conn.close();
                } catch (SQLException e) {
                }
        }
    }
}
