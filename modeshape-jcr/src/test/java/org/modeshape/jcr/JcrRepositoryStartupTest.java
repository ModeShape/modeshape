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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.query.Query;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Editor;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.connector.mock.MockConnector;
import org.modeshape.jcr.api.Workspace;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.document.DocumentStore;
import org.modeshape.jcr.query.JcrQuery;
import org.modeshape.jcr.security.SimplePrincipal;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.binary.FileSystemBinaryStore;

/**
 * Tests that related to repeatedly starting/stopping repositories (without another repository configured in the @Before and @After
 * methods).
 * 
 * @author rhauch
 * @author hchiorean
 */
public class JcrRepositoryStartupTest extends MultiPassAbstractTest {

    @Test
    @FixFor( {"MODE-1526", "MODE-1512", "MODE-1617"} )
    public void shouldKeepPersistentDataAcrossRestart() throws Exception {
        FileUtil.delete("target/persistent_repository/");

        String repositoryConfigFile = "config/repo-config-persistent-cache.json";

        startRunStop(repository -> {
            Session session = repository.login();
            session.getRootNode().addNode("testNode");
            session.save();

            // create 2 new workspaces
            session.getWorkspace().createWorkspace("ws1");
            session.getWorkspace().createWorkspace("ws2");
            session.logout();
        }, repositoryConfigFile);

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
        }, repositoryConfigFile);

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
        }, repositoryConfigFile);
    }

    @Test
    public void shouldNotImportInitialContentIfWorkspaceContentsChanged() throws Exception {
        // remove the ISPN local data, so we always start fresh
        FileUtil.delete("target/persistent_repository_initial_content");

        String repositoryConfigFile = "config/repo-config-persistent-cache-initial-content.json";
        startRunStop(repository -> {
            Session ws1Session = repository.login("ws1");
            // check the initial import
            Node node = ws1Session.getNode("/cars");
            assertNotNull(node);
            // remove the node initially imported and add a new one
            node.remove();
            ws1Session.getRootNode().addNode("testNode");

            ws1Session.save();
        }, repositoryConfigFile);

        startRunStop(repository -> {
            Session ws1Session = repository.login("ws1");
            try {
                ws1Session.getNode("/cars");
                fail("The initial content should be be re-imported if a workspace is not empty");
            } catch (PathNotFoundException e) {
                // expected
            }
            ws1Session.getNode("/testNode");
        }, repositoryConfigFile);
    }

    @FixFor( "MODE-1693" )
    @Test( expected = ConfigurationException.class )
    public void shouldNotStartIfTransactionsArentEnabled() throws Exception {
        startRunStop(null, "config/invalid-repo-config-no-transactions.json");
    }

    @Test
    @FixFor( "MODE-1716" )
    public void shouldPersistExternalProjectionToFederatedNodeMappings() throws Exception {
        FileUtil.delete("target/federation_persistent_repository");

        String repositoryConfigFile = "config/repo-config-mock-federation-persistent.json";
        startRunStop(repository -> {
            Session session = repository.login();
            Node testRoot = session.getRootNode().addNode("testRoot");
            session.save();

            FederationManager federationManager = ((Workspace)session.getWorkspace()).getFederationManager();

            federationManager.createProjection("/testRoot", "mock-source", MockConnector.DOC1_LOCATION, "federated1");
            federationManager.createProjection("/testRoot", "mock-source", MockConnector.DOC2_LOCATION, null);
            Node doc1Federated = session.getNode("/testRoot/federated1");
            assertNotNull(doc1Federated);
            assertEquals(testRoot.getIdentifier(), doc1Federated.getParent().getIdentifier());
        }, repositoryConfigFile);

        startRunStop(repository -> {
            Session session = repository.login();
            Node testRoot = session.getNode("/testRoot");
            assertNotNull(testRoot);

            Node doc1Federated = session.getNode("/testRoot/federated1");
            assertNotNull(doc1Federated);
            assertEquals(testRoot.getIdentifier(), doc1Federated.getParent().getIdentifier());

            Node doc2Federated = session.getNode("/testRoot" + MockConnector.DOC2_LOCATION);
            assertNotNull(doc2Federated);
            assertEquals(testRoot.getIdentifier(), doc2Federated.getParent().getIdentifier());
        }, repositoryConfigFile);
    }

    @Test
    public void shouldKeepPreconfiguredProjectionsAcrossRestart() throws Exception {
        FileUtil.delete("target/federation_persistent_repository");

        String repositoryConfigFile = "config/repo-config-federation-persistent-projections.json";
        RepositoryOperation checkPreconfiguredProjection = repository -> {
            Session session = repository.login();
            assertNotNull(session.getNode("/preconfiguredProjection"));
        };
        startRunStop(checkPreconfiguredProjection, repositoryConfigFile);
        startRunStop(checkPreconfiguredProjection, repositoryConfigFile);
    }

    @Test
    public void shouldCleanStoredProjectionsIfNodesAreDeleted() throws Exception {
        FileUtil.delete("target/federation_persistent_repository");

        String repositoryConfigFile = "config/repo-config-mock-federation-persistent.json";
        startRunStop(repository -> {
            Session session = repository.login();
            session.getRootNode().addNode("testRoot");
            session.save();

            FederationManager federationManager = ((Workspace)session.getWorkspace()).getFederationManager();
            federationManager.createProjection("/testRoot", "mock-source", MockConnector.DOC1_LOCATION, "federated1");
            federationManager.createProjection("/testRoot", "mock-source", MockConnector.DOC2_LOCATION, "federated2");
            Node projection = session.getNode("/testRoot/federated1");
            assertNotNull(projection);

            projection.remove();
            session.save();
        }, repositoryConfigFile);

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
        }, repositoryConfigFile);
    }

    @Test
    @FixFor( "MODE-1844" )
    public void shouldNotRemainInInconsistentStateIfErrorsOccurOnStartup() throws Exception {
        FileUtil.delete("target/persistent_repository_initial_content");
        // try and start with a config that will produce an exception
        try {
            startRunStop(repository -> {}, "config/invalid-repo-config-persistent-initial-content.json");
            fail("Expected a repository exception");
        } catch (RuntimeException e) {
            if (!(e.getCause() instanceof RepositoryException)) {
                throw e;
            }
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            executorService.submit(()-> startRunStop(repository -> {}, "config/repo-config-persistent-cache-initial-content.json"))
                            .get(10, TimeUnit.SECONDS);
            // wait the repo to restart or fail
        } catch (java.util.concurrent.TimeoutException e) {
            fail("Repository did not restart in the expected amount of time");
        }
        finally {
            executorService.shutdownNow();
        }
    }

    @Test
    @FixFor( "MODE-2031" )
    public void shouldRestartWithModifiedCNDFile() throws Exception {
        FileUtil.delete("target/persistent_repository/");
        startRunStop(repository -> {
            Session session = repository.login();
            Node content = session.getRootNode().addNode("content", "jj:content");
            content.setProperty("_name", "name");
            content.setProperty("_type", "type");
            session.save();
        }, "config/repo-config-jj-initial.json");

        RepositoryOperation operation = (repository) -> {
            Session session = repository.login();
            Node content = session.getNode("/content");
            assertEquals("name", content.getProperty("_name").getString());
            assertEquals("type", content.getProperty("_type").getString());
        };
        startRunStop(operation, "config/repo-config-jj-modified.json");
        startRunStop(operation, "config/repo-config-jj-modified.json");
    }

    @Test
    @FixFor( "MODE-2044" )
    public void shouldRun3_6_0UpgradeFunction() throws Exception {
        FileUtil.delete("target/persistent_repository/");
        // first run is empty, so no upgrades will be performed
        startRunStop(repository ->  {
            // modify the repository-info document to force an upgrade on the next restart
            changeLastUpgradeId(repository, Upgrades.ModeShape_3_6_0.INSTANCE.getId() - 1);

            // create a non-session lock on a node
            JcrSession session = repository.login();
            PropertyFactory propertyFactory = session.context().getPropertyFactory();
            AbstractJcrNode node = session.getRootNode().addNode("/test");
            node.addMixin("mix:lockable");
            session.save();
            session.lockManager().lock(node, true, false, Long.MAX_VALUE, null);

            // manipulate that lock using the system cache to simulate corrupt data
            SessionCache systemSession = repository.createSystemSession(repository.runningState().context(), false);
            SystemContent systemContent = new SystemContent(systemSession);
            ChildReferences childReferences = systemContent.locksNode().getChildReferences(systemSession);
            assertFalse("No locks found", childReferences.isEmpty());
            for (ChildReference childReference : childReferences) {
                MutableCachedNode lock = systemSession.mutable(childReference.getKey());
                lock.setProperty(systemSession, propertyFactory.create(ModeShapeLexicon.IS_DEEP, true));
                lock.setProperty(systemSession, propertyFactory.create(ModeShapeLexicon.LOCKED_KEY, node.key().toString()));
                lock.setProperty(systemSession, propertyFactory.create(ModeShapeLexicon.SESSION_SCOPE, false));
            }
            systemSession.save();
        }, "config/repo-config-persistent-no-indexes.json");

        // second run should run the upgrade
        startRunStop(repository -> {
            // manipulate that lock using the system cache to simulate corrupt data
            SessionCache systemSession = repository.createSystemSession(repository.runningState().context(), true);
            SystemContent systemContent = new SystemContent(systemSession);
            ChildReferences childReferences = systemContent.locksNode().getChildReferences(systemSession);
            assertFalse("No locks found", childReferences.isEmpty());
            for (ChildReference childReference : childReferences) {
                CachedNode lock = systemSession.getNode(childReference.getKey());
                assertNull("Property not removed", lock.getProperty(ModeShapeLexicon.IS_DEEP, systemSession));
                assertNull("Property not removed", lock.getProperty(ModeShapeLexicon.LOCKED_KEY, systemSession));
                assertNull("Property not removed", lock.getProperty(ModeShapeLexicon.SESSION_SCOPE, systemSession));
            }
        }, "config/repo-config-persistent-no-indexes.json");
    }

    @Test
    @FixFor( "MODE-2049" )
    public void shouldStartAndStopRepositoryWithoutMonitoringConfigured() throws Exception {
        startRunStop(repository -> repository.login().logout(), "config/repo-config-inmemory-local-environment-no-monitoring.json");
    }

    @Test
    @FixFor( "MODE-1683" )
    public void shouldAppendJournalEntriesBetweenRestarts() throws Exception {
        FileUtil.delete("target/journal/");
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
        startRunStop(operation, "config/repo-config-journaling.json");
        startRunStop(operation, "config/repo-config-journaling.json");

        int countFirstTime = recordsOnStartup.get(0);
        int countSecondTime = recordsOnStartup.get(1);
        assertTrue(countSecondTime > countFirstTime);
    }

    @Test
    @FixFor( "MODE-2100" )
    public void shouldAddPredefinedWorkspacesOnRestartViaConfigUpdate() throws Exception {
        FileUtil.delete("target/persistent_repository/");
        URL configUrl = getClass().getClassLoader().getResource("config/repo-config-persistent-predefined-ws.json");
        RepositoryConfiguration config = RepositoryConfiguration.read(configUrl);
        startRunStop(repository -> {
            JcrSession session = repository.login("default");
            session.logout();
            session = repository.login("ws1");
            session.getWorkspace().createWorkspace("ws2");
            session.logout();
            session = repository.login("ws2");
            session.logout();
        }, config);

        final Editor editor = config.edit();
        EditableArray predefinedWs = editor.getDocument(RepositoryConfiguration.FieldName.WORKSPACES)
                                           .getArray(RepositoryConfiguration.FieldName.PREDEFINED);
        predefinedWs.add("ws3");
        predefinedWs.add("ws4");

        startRunStop(repository -> {
            repository.apply(editor.getChanges());
            JcrSession session = repository.login("default");
            session.logout();
            session = repository.login("ws1");
            session.logout();
            session = repository.login("ws2");
            session.logout();
            session = repository.login("ws3");
            session.logout();
            session = repository.login("ws4");
            session.logout();
        }, config);
    }

    @Test
    @FixFor( "MODE-2100" )
    public void shouldAddPredefinedWorkspacesOnRestartViaConfigChange() throws Exception{
        FileUtil.delete("target/persistent_repository/");
        startRunStop(repository -> {
            JcrSession session = repository.login("ws1");
            session.getWorkspace().createWorkspace("ws3");
            session.logout();
            session = repository.login("ws3");
            session.logout();
        }, "config/repo-config-persistent-predefined-ws.json");

        startRunStop(repository -> {
            JcrSession session = repository.login("default");
            session.logout();
            session = repository.login("ws1");
            session.logout();
            session = repository.login("ws2");
            session.logout();
            session = repository.login("ws3");
            session.logout();
        }, "config/repo-config-persistent-predefined-ws-update.json");

        startRunStop(repository -> repository.login("ws2").logout(), "config/repo-config-persistent-predefined-ws.json");
    }

    @Test
    @FixFor( "MODE-2142" )
    public void shouldAllowChangingNamespacePrefixesInSession() throws Exception {
        FileUtil.delete("target/persistent_repository/");

        String repositoryConfigFile = "config/repo-config-persistent-cache.json";

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
        }, repositoryConfigFile);

        startRunStop(repository -> {
            Session session = repository.login();
            Node node = session.getNode("/testNode");
            assertEquals(nodeTypeName, node.getPrimaryNodeType().getName());
        }, repositoryConfigFile);
    }

    @Test
    @FixFor( "MODE-2167" )
    public void shouldDisableACLsIfAllPoliciesAreRemoved() throws Exception {
        FileUtil.delete("target/persistent_repository/");

        String repositoryConfigFile = "config/repo-config-persistent-cache.json";

        startRunStop(repository -> {
            Session session = repository.login();
            Node testNode = session.getRootNode().addNode("testNode");
            testNode.addNode("node1");
            testNode.addNode("node2");
            session.save();

            AccessControlManager acm = session.getAccessControlManager();

            AccessControlList aclNode1 = getACL(acm, "/testNode/node1");
            aclNode1.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                                           new Privilege[] {acm.privilegeFromName(Privilege.JCR_ALL)});
            acm.setPolicy("/testNode/node1", aclNode1);

            AccessControlList aclNode2 = getACL(acm, "/testNode/node2");
            aclNode2.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                                           new Privilege[] {acm.privilegeFromName(Privilege.JCR_ALL)});
            acm.setPolicy("/testNode/node2", aclNode2);

            // access control should not be enabled yet because we haven't saved the session
            assertFalse(repository.runningState().repositoryCache().isAccessControlEnabled());

            session.save();
            assertTrue(repository.runningState().repositoryCache().isAccessControlEnabled());
        }, repositoryConfigFile);

        startRunStop(repository -> {
            assertTrue(repository.runningState().repositoryCache().isAccessControlEnabled());

            Session session = repository.login();
            AccessControlManager acm = session.getAccessControlManager();
            // TODO author=Horia Chiorean date=25-Mar-14 description=Why null here !?!
            acm.removePolicy("/testNode/node1", null);
            acm.removePolicy("/testNode/node2", null);
            session.save();

            assertFalse(repository.runningState().repositoryCache().isAccessControlEnabled());

            session.getNode("/testNode").remove();
            session.save();
        }, repositoryConfigFile);
    }

    @Test
    @FixFor( "MODE-2167" )
    public void shouldRun4_0_0_Alpha1_UpgradeFunction() throws Exception {
        FileUtil.delete("target/persistent_repository/");
        String config = "config/repo-config-persistent-no-indexes.json";
        // first run is empty, so no upgrades will be performed
        startRunStop(repository -> {
            changeLastUpgradeId(repository, Upgrades.ModeShape_4_0_0_Alpha1.INSTANCE.getId() - 1);

            // modify some ACLs
            JcrSession session = repository.login();
            session.getRootNode().addNode("testNode");
            session.save();

            AccessControlManager acm = session.getAccessControlManager();

            AccessControlList acl = getACL(acm, "/testNode");
            acl.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                                      new Privilege[] {acm.privilegeFromName(Privilege.JCR_ALL)});
            acm.setPolicy("/testNode", acl);
            session.save();

            // remove the new property from 4.0 which actually stores the ACL count to simulate a pre 4.0 repository
            SessionCache systemSession = repository.createSystemSession(repository.runningState().context(), false);
            SystemContent systemContent = new SystemContent(systemSession);
            MutableCachedNode systemNode = systemContent.mutableSystemNode();
            systemNode.removeProperty(systemSession, ModeShapeLexicon.ACL_COUNT);
            systemSession.save();
        }, config);

        // second run should run the upgrade
        startRunStop(repository -> {
            // check that the upgrade function correctly added the new property
            SessionCache systemSession = repository.createSystemSession(repository.runningState().context(), false);
            SystemContent systemContent = new SystemContent(systemSession);
            MutableCachedNode systemNode = systemContent.mutableSystemNode();
            Property aclCountProp = systemNode.getProperty(ModeShapeLexicon.ACL_COUNT, systemSession);
            assertNotNull("ACL count property not found after upgrade", aclCountProp);
            assertEquals(1, Long.valueOf(aclCountProp.getFirstValue().toString()).longValue());

            // force a 2nd upgrade
            changeLastUpgradeId(repository, Upgrades.ModeShape_4_0_0_Alpha1.INSTANCE.getId() - 1);

            // remove all ACLs
            JcrSession session = repository.login();
            AccessControlManager acm = session.getAccessControlManager();
            // TODO author=Horia Chiorean date=25-Mar-14 description=Why null ?!
            acm.removePolicy("/testNode", null);
            session.save();

            // remove the new property from 4.0 which actually stores the ACL count to simulate a pre 4.0 repository
            systemNode.removeProperty(systemSession, ModeShapeLexicon.ACL_COUNT);
            systemSession.save();
        }, config);

        // check that the upgrade disabled ACLs
        startRunStop(repository -> {
            SessionCache systemSession = repository.createSystemSession(repository.runningState().context(), true);
            SystemContent systemContent = new SystemContent(systemSession);
            CachedNode systemNode = systemContent.systemNode();
            Property aclCountProp = systemNode.getProperty(ModeShapeLexicon.ACL_COUNT, systemSession);
            assertNotNull("ACL count property not found after upgrade", aclCountProp);
            assertEquals(0, Long.valueOf(aclCountProp.getFirstValue().toString()).longValue());

            assertFalse(repository.runningState().repositoryCache().isAccessControlEnabled());
        }, config);
    }

    @Test
    @FixFor( "MODE-2176" )
    public void shouldAllowExternalSourceChangesBetweenRestarts() throws Exception {
        FileUtil.delete("target/persistent_repository/");

        prepareExternalDirectory("target/federation_persistent_1");
        startRunStop(repository-> {
            JcrSession session = repository.login();
            assertNotNull(session.getNode("/fs1"));
            assertNotNull(session.getNode("/fs1/file.txt"));
            assertNotNull(session.getNode("/fs2"));
            assertNotNull(session.getNode("/fs2/file.txt"));
        }, "config/repo-config-persistent-cache-fs-connector1.json");

        FileUtil.delete("target/federation_persistent_1");
        prepareExternalDirectory("target/federation_persistent_2");
        // restart with a configuration file which a) has a new external source, b) has changed the directory path of "fs2" and
        // c) has removed the fs1 projection
        startRunStop(repository -> {
            JcrSession session = repository.login();
            try {
                session.getNode("/fs1");
                fail("The projection should not have been found");
            } catch (PathNotFoundException e) {
                // expected
            }
            assertNotNull(session.getNode("/fs2"));
            assertNotNull(session.getNode("/fs2/file.txt"));
        }, "config/repo-config-persistent-cache-fs-connector2.json");
    }

    @Test
    @FixFor( "MODE-2302" )
    public void shouldRun4_0_0_Beta3_UpgradeFunction() throws Exception {
        FileUtil.delete("target/legacy_fs_binarystore");
        FileUtil.delete("target/persistent_repository/");
        String config = "config/repo-config-persistent-legacy-fsbinary.json";
        // copy the test-resources legacy structure onto the configured one
        FileUtil.copy(new File("src/test/resources/legacy_fs_binarystore"), new File("target/legacy_fs_binarystore"));
        // this is coming from the test resources
        final BinaryKey binaryKey = new BinaryKey("ef2138973a86a8929eebe7bf52419b7cde73ba0a");
        // first run is empty, so no upgrades will be performed but we'll decrement the last upgrade ID to force an upgrade next
        // restart
        startRunStop(repository -> {
            changeLastUpgradeId(repository, Upgrades.ModeShape_4_0_0_Beta3.INSTANCE.getId() - 1);
            FileSystemBinaryStore binaryStore = (FileSystemBinaryStore)repository.runningState().binaryStore();
            assertFalse("No used binaries expected", binaryStore.getAllBinaryKeys().iterator().hasNext());
            assertFalse("The binary should not be found", binaryStore.hasBinary(binaryKey));
            File mainStorageDirectory = binaryStore.getDirectory();
            File[] files = mainStorageDirectory.listFiles();
            assertEquals("Just the trash directory was expected", 1, files.length);
            File trash = files[0];
            assertTrue(trash.isDirectory());
        }, config);

        // run the repo a second time, which should run the upgrade
        startRunStop(repository -> {
            FileSystemBinaryStore binaryStore = (FileSystemBinaryStore)repository.runningState().binaryStore();
            assertFalse("No used binaries expected", binaryStore.getAllBinaryKeys().iterator().hasNext());
            assertTrue("The binary should be found", binaryStore.hasBinary(binaryKey));
        }, config);
    }

    @Test
    @FixFor( "MODE-2341" )
    public void shouldAllowReindexingWithLocalProviderBetweenRestartsWhenMissing() throws Exception {
        // clean the main repo data
        FileUtil.delete("target/persistent_repository");

        // clean the indexes
        FileUtil.delete("target/startup_test_indexes");

        // setup the external content
        prepareExternalDirectory("target/federation_persistent_2");

        RepositoryOperation reindexingExternalContentOperation = reindexingExternalContentOperation();

        // run 1
        startRunStop(reindexingExternalContentOperation, "config/repo-config-persistent-cache-fs-connector2.json");
        long indexFolderSize1 = FileUtil.size("target/startup_test_indexes");

        // clean the indexes
        assertTrue(FileUtil.delete("target/startup_test_indexes"));

        // run 2
        startRunStop(reindexingExternalContentOperation, "config/repo-config-persistent-cache-fs-connector2.json");
        long indexFolderSize2 = FileUtil.size("target/startup_test_indexes");

        assertEquals("The sizes of the index folder are different between 2 identical reindex runs", indexFolderSize1,
                     indexFolderSize2);
    }

    private RepositoryOperation reindexingExternalContentOperation() {
        return repository -> {
            JcrSession session = repository.login();
            // sleep a bit to make sure reindexing completes
            Thread.sleep(100);
            try {
                AbstractJcrNode node = session.getNode("/fs2/file.txt");
                String createdBy = node.getProperty("jcr:createdBy").getString();
                assertNotNull(createdBy);
                JcrQueryManager jcrQueryManager = session.getWorkspace().getQueryManager();
                Query query = jcrQueryManager.createQuery("select file.[jcr:path] from [nt:file] as file where file.[jcr:createdBy]='" +
                                                          createdBy + "'",
                                                          JcrQuery.JCR_SQL2);
                ValidateQuery.validateQuery()
                             .useIndex("nodesByAuthor")
                             .hasNodesAtPaths("/fs2/file.txt")
                             .validate(query, query.execute());

                session.getWorkspace().reindex();
                ValidateQuery.validateQuery()
                             .useIndex("nodesByAuthor")
                             .hasNodesAtPaths("/fs2/file.txt")
                             .validate(query, query.execute());
            } finally {
                session.logout();
            }
        };
    }

    @Test
    @FixFor( "MODE-2391" )
    public void shouldNotReindexBetweenRestartsLocalProviderIfExists() throws Exception {
        // clean the main repo data
        FileUtil.delete("target/persistent_repository");
        // clean the indexes
        FileUtil.delete("target/startup_test_indexes");
        startRunStop(repository -> {
            long initialSize = FileUtil.size("target/startup_test_indexes");
            JcrSession session = repository.login();
            int nodeCount = 100;
            for (int i = 0; i < nodeCount; i++) {
                session.getRootNode().addNode("node_" + i);
            }
            session.save();  
            session.logout();
            // the indexes are sync, so the FS size should've increased because of the new nodes
            assertTrue(initialSize < FileUtil.size("target/startup_test_indexes"));
        }, "config/repo-config-persistent-local-indexes.json");
       
        long indexFolderSize = FileUtil.size("target/startup_test_indexes"); 
        assertTrue(indexFolderSize > 0);
        startRunStop(repository -> {
            // wait a bit so that if reindexing was happening it would be finished before shutting down
            Thread.sleep(200);
            repository.login().logout();
        }, "config/repo-config-persistent-local-indexes.json");
        // if reindexing was happening, it would be async so the next assert would normally fail
        assertEquals("Re-indexing should not be happening", indexFolderSize, FileUtil.size("target/startup_test_indexes"));
    }

    @Test
    @FixFor( "MODE-2393 ")
    public void reindexingLocalProviderShouldRemoveExistingDataFirst() throws Exception {
        // clean the main repo data
        FileUtil.delete("target/persistent_repository");
        // clean the indexes
        FileUtil.delete("target/startup_test_indexes");
        startRunStop(repository -> {
            JcrSession session = repository.login();
            session.getRootNode().addNode("testRoot");
            session.save();
            String sql = "select [jcr:path] from [nt:unstructured] where [jcr:name] = 'testRoot'";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            ValidateQuery.validateQuery().rowCount(1).useIndex("nodesByName").validate(query, query.execute());
            session.logout();
        }, "config/repo-config-persistent-local-indexes.json");
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
        }, "config/repo-config-persistent-local-indexes.json");
    }

    private void prepareExternalDirectory( String dirpath ) throws IOException {
        FileUtil.delete(dirpath);
        new File(dirpath).mkdir();
        File file = new File(dirpath + "/file.txt");
        IoUtil.write(JcrRepositoryStartupTest.class.getClassLoader().getResourceAsStream("io/file1.txt"),
                     new FileOutputStream(file));
    }

    protected void changeLastUpgradeId( JcrRepository repository,
                                        int value ) throws Exception {
        // modify the repository-info document to force an upgrade on the next restart
        DocumentStore documentStore = repository.documentStore();
        repository.transactionManager().begin();
        EditableDocument editableDocument = documentStore.localStore().edit("repository:info", true);
        editableDocument.set("lastUpgradeId", value);
        documentStore.localStore().put("repository:info", editableDocument);
        repository.transactionManager().commit();
    }

    protected AccessControlList getACL( AccessControlManager acm,
                                        String absPath ) throws Exception {
        AccessControlPolicyIterator it = acm.getApplicablePolicies(absPath);
        if (it.hasNext()) {
            return (AccessControlList)it.nextAccessControlPolicy();
        }
        return (AccessControlList)acm.getPolicies(absPath)[0];
    }

}
