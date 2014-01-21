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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
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
import javax.jcr.query.QueryManager;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Editor;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.connector.mock.MockConnector;
import org.modeshape.jcr.api.Workspace;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.document.DocumentStore;
import org.modeshape.jcr.value.PropertyFactory;

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

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                session.getRootNode().addNode("testNode");
                session.save();

                // create 2 new workspaces
                session.getWorkspace().createWorkspace("ws1");
                session.getWorkspace().createWorkspace("ws2");
                session.logout();

                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {

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

                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
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
                return null;
            }
        }, repositoryConfigFile);
    }

    @Test
    public void shouldNotImportInitialContentIfWorkspaceContentsChanged() throws Exception {
        // remove the ISPN local data, so we always start fresh
        FileUtil.delete("target/persistent_repository_initial_content");

        String repositoryConfigFile = "config/repo-config-persistent-cache-initial-content.json";
        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                JcrSession ws1Session = repository.login("ws1");
                // check the initial import
                AbstractJcrNode node = ws1Session.getNode("/cars");
                assertNotNull(node);
                // remove the node initially imported and add a new one
                node.remove();
                ws1Session.getRootNode().addNode("testNode");

                ws1Session.save();
                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                JcrSession ws1Session = repository.login("ws1");
                try {
                    ws1Session.getNode("/cars");
                    fail("The initial content should be be re-imported if a workspace is not empty");
                } catch (PathNotFoundException e) {
                    // expected
                }
                ws1Session.getNode("/testNode");
                return null;
            }
        }, repositoryConfigFile);
    }

    @FixFor( "MODE-1693" )
    @Test( expected = ConfigurationException.class )
    public void shouldNotStartIfTransactionsArentEnabled() throws Exception {
        startRunStop(null, "config/invalid-repo-config-no-transactions.json");
    }

    @FixFor( "MODE-1899" )
    @Test( expected = ConfigurationException.class )
    public void shouldNotStartIfDefaultWorkspaceCacheIsTransactional() throws Exception {
        startRunStop(null, "config/invalid-repo-config-tx-default-ws-cache.json");
    }

    @FixFor( "MODE-1899" )
    @Test
    public void shouldFailWhenCreatingWorkspaceWithTransactionalCache() throws Exception {
        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                try {
                    session.getWorkspace().createWorkspace("ws1");
                    fail("It should not be possible to create a workspace which has a transactional cache configured");
                } catch (ConfigurationException e) {
                    //expected
                }
                return null;
            }
        }, "config/invalid-repo-config-tx-ws-cache.json");
    }

    @Test
    @FixFor( "MODE-1716" )
    public void shouldPersistExternalProjectionToFederatedNodeMappings() throws Exception {
        FileUtil.delete("target/federation_persistent_repository");

        String repositoryConfigFile = "config/repo-config-mock-federation-persistent.json";
        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                Node testRoot = session.getRootNode().addNode("testRoot");
                session.save();

                FederationManager federationManager = ((Workspace)session.getWorkspace()).getFederationManager();

                federationManager.createProjection("/testRoot",
                                                   "mock-source",
                                                   MockConnector.DOC1_LOCATION,
                                                   "federated1");
                federationManager.createProjection("/testRoot",
                                                   "mock-source",
                                                   MockConnector.DOC2_LOCATION,
                                                   null);
                Node doc1Federated = session.getNode("/testRoot/federated1");
                assertNotNull(doc1Federated);
                assertEquals(testRoot.getIdentifier(), doc1Federated.getParent().getIdentifier());

                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                Node testRoot = session.getNode("/testRoot");
                assertNotNull(testRoot);

                Node doc1Federated = session.getNode("/testRoot/federated1");
                assertNotNull(doc1Federated);
                assertEquals(testRoot.getIdentifier(), doc1Federated.getParent().getIdentifier());

                Node doc2Federated = session.getNode("/testRoot" + MockConnector.DOC2_LOCATION);
                assertNotNull(doc2Federated);
                assertEquals(testRoot.getIdentifier(), doc2Federated.getParent().getIdentifier());
                return null;
            }
        }, repositoryConfigFile);
    }

    @Test
    public void shouldKeepPreconfiguredProjectionsAcrossRestart() throws Exception {
        FileUtil.delete("target/federation_persistent_repository");

        String repositoryConfigFile = "config/repo-config-federation-persistent-projections.json";
        RepositoryOperation checkPreconfiguredProjection = new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                assertNotNull(session.getNode("/preconfiguredProjection"));
                return null;
            }
        };
        startRunStop(checkPreconfiguredProjection, repositoryConfigFile);
        startRunStop(checkPreconfiguredProjection, repositoryConfigFile);
    }

    @Test
    public void shouldCleanStoredProjectionsIfNodesAreDeleted() throws Exception {
        FileUtil.delete("target/federation_persistent_repository");

        String repositoryConfigFile = "config/repo-config-mock-federation-persistent.json";
        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                session.getRootNode().addNode("testRoot");
                session.save();

                FederationManager federationManager = ((Workspace)session.getWorkspace()).getFederationManager();
                federationManager.createProjection("/testRoot",
                                                   "mock-source",
                                                   MockConnector.DOC1_LOCATION,
                                                   "federated1");
                federationManager.createProjection("/testRoot",
                                                   "mock-source",
                                                   MockConnector.DOC2_LOCATION,
                                                   "federated2");
                Node projection = session.getNode("/testRoot/federated1");
                assertNotNull(projection);

                projection.remove();
                session.save();
                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                //check the 2nd projection
                assertNotNull(session.getNode("/testRoot/federated2"));
                try {
                    session.getNode("/testRoot/federated1");
                    fail("Projection has not been cleaned up");
                } catch (PathNotFoundException e) {
                    //expected
                }
                return null;
            }
        }, repositoryConfigFile);
    }

    @Test
    @FixFor( "MODE-1785" )
    public void shouldRebuildIndexesIfConfiguredTo() throws Exception {
        FileUtil.delete("target/persistent_repository");

        String repositoryConfigFile = "config/repo-config-persistent-always-rebuild-indexes.json";
        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                session.getRootNode().addNode("testNode");
                session.save();

                QueryManager queryManager = session.getWorkspace().getQueryManager();
                Query query = queryManager.createQuery("select * from [nt:base] where [jcr:path] like '/testNode'", Query.JCR_SQL2);
                assertEquals(1, query.execute().getNodes().getSize());

                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();

                QueryManager queryManager = session.getWorkspace().getQueryManager();
                Query query = queryManager.createQuery("select * from [nt:unstructured] where [jcr:path] like '/testNode'", Query.JCR_SQL2);
                assertEquals(1, query.execute().getNodes().getSize());

                return null;
            }
        }, repositoryConfigFile);
    }

    @Test
    @FixFor( "MODE-1844" )
    public void shouldNotRemainInInconsistentStateIfErrorsOccurOnStartup() throws Exception {
        FileUtil.delete("target/persistent_repository_initial_content");
        //try and start with a config that will produce an exception
        String repositoryConfigFile = "config/invalid-repo-config-persistent-initial-content.json";
        try {
            startRunStop(new RepositoryOperation() {
                @Override
                public Void call() throws Exception {
                    return null;
                }
            }, repositoryConfigFile);
            fail("Expected a repository exception");
        } catch (RepositoryException e) {
            //expected
        }

        final CountDownLatch restartLatch = new CountDownLatch(1);
        Callable<Void> restartRunnable = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                startRunStop(new RepositoryOperation() {
                    @Override
                    public Void call() throws Exception {
                        restartLatch.countDown();
                        return null;
                    }
                }, "config/repo-config-persistent-cache-initial-content.json");
                return null;
            }
        };
        Executors.newSingleThreadExecutor().submit(restartRunnable);
        //wait the repo to restart or fail
        assertTrue("Repository did not restart in the expected amount of time", restartLatch.await(1, TimeUnit.MINUTES));
    }

    @Test
    @FixFor( "MODE-1872" )
    public void asyncReindexingWithoutSystemContentShouldNotCorruptSystemBranch() throws Exception {
        FileUtil.delete("target/persistent_repository/");
        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                JcrSession session = repository.login();

                javax.jcr.Node root = session.getRootNode();
                AbstractJcrNode system = (AbstractJcrNode)root.getNode("jcr:system");
                assertThat(system, is(notNullValue()));
                return null;
            }
        }, "config/repo-config-persistent-indexes-always-async-without-system.json");
    }

    @Test
    @FixFor("MODE-2031")
    public void shouldRestartWithModifiedCNDFile() throws Exception {
        FileUtil.delete("target/persistent_repository/");
        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                JcrSession session = repository.login();
                Node content = session.getRootNode().addNode("content", "jj:content");
                content.setProperty("_name", "name");
                content.setProperty("_type", "type");
                session.save();
                return null;
            }
        }, "config/repo-config-jj-initial.json");

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                JcrSession session = repository.login();
                Node content = session.getNode("/content");
                assertEquals("name", content.getProperty("_name").getString());
                assertEquals("type", content.getProperty("_type").getString());
                return null;
            }
        }, "config/repo-config-jj-modified.json");

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                JcrSession session = repository.login();
                Node content = session.getNode("/content");
                assertEquals("name", content.getProperty("_name").getString());
                assertEquals("type", content.getProperty("_type").getString());
                return null;
            }
        }, "config/repo-config-jj-modified.json");
    }

    @Test
    @FixFor("MODE-2044")
    public void shouldRun3_6_0UpgradeFunction() throws Exception {
        FileUtil.delete("target/persistent_repository/");
        //first run is empty, so no upgrades will be performed
        startRunStop(new RepositoryOperation() {
            @SuppressWarnings( "deprecation" )
            @Override
            public Void call() throws Exception {
                //modify the repository-info document to force an upgrade on the next restart
                DocumentStore documentStore =  repository.documentStore();
                EditableDocument editableDocument = documentStore.localStore().get("repository:info").editDocumentContent();
                editableDocument.set("lastUpgradeId", Upgrades.ModeShape_3_6_0.INSTANCE.getId() - 1);
                documentStore.localStore().put("repository:info", editableDocument);

                //create a non-session lock on a node
                JcrSession session = repository.login();
                PropertyFactory propertyFactory = session.context().getPropertyFactory();
                AbstractJcrNode node = session.getRootNode().addNode("/test");
                node.addMixin("mix:lockable");
                session.save();
                session.lockManager().lock(node, true, false, Long.MAX_VALUE, null);

                //manipulate that lock using the system cache to simulate corrupt data
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
                return null;
            }
        }, "config/repo-config-persistent-no-query.json");

        //second run should run the upgrade
        startRunStop(new RepositoryOperation() {
            @SuppressWarnings( "deprecation" )
            @Override
            public Void call() throws Exception {
                //manipulate that lock using the system cache to simulate corrupt data
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
                return null;
            }
        }, "config/repo-config-persistent-no-query.json");
    }

    @Test
    @FixFor( "MODE-2049" )
    public void shouldStartAndStopRepositoryWithoutMonitoringConfigured() throws Exception {
        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                repository.login().logout();
                return null;
            }
        }, "config/repo-config-inmemory-local-environment-no-monitoring.json");
    }

    @Test
    @FixFor( "MODE-1683" )
    public void shouldAppendJournalEntriesBetweenRestarts() throws Exception {
        FileUtil.delete("target/journal/");
        final List<Integer> recordsOnStartup = new ArrayList<Integer>(2);
        startRunStop( new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                session.getRootNode().addNode("node1");
                session.save();
                session.logout();
                Thread.sleep(100);
                int records = repository.runningState().journal().allRecords(false).size();
                assertTrue(records > 0);
                recordsOnStartup.add(records);
                return null;
            }
        }, "config/repo-config-journaling.json");

        startRunStop( new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                session.getRootNode().addNode("node1");
                session.save();
                session.logout();
                Thread.sleep(100);
                int records = repository.runningState().journal().allRecords(false).size();
                assertTrue(records > 0);
                recordsOnStartup.add(records);
                return null;
            }
        }, "config/repo-config-journaling.json");

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
        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                JcrSession session  = repository.login("default");
                session.logout();
                session = repository.login("ws1");
                session.getWorkspace().createWorkspace("ws2");
                session.logout();
                session = repository.login("ws2");
                session.logout();
                return null;
            }
        }, config);

        final Editor editor = config.edit();
        EditableArray predefinedWs = editor.getDocument(RepositoryConfiguration.FieldName.WORKSPACES).getArray(RepositoryConfiguration.FieldName.PREDEFINED);
        predefinedWs.add("ws3");
        predefinedWs.add("ws4");

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                repository.apply(editor.getChanges());

                JcrSession session  = repository.login("default");
                session.logout();
                session = repository.login("ws1");
                session.logout();
                session = repository.login("ws2");
                session.logout();
                session = repository.login("ws3");
                session.logout();
                session = repository.login("ws4");
                session.logout();
                return null;
            }
        }, config);
    }

    @Test
    @FixFor( "MODE-2100" )
    public void shouldAddPredefinedWorkspacesOnRestartViaConfigChange() throws Exception {
        FileUtil.delete("target/persistent_repository/");
        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                JcrSession session  = repository.login("ws1");
                session.getWorkspace().createWorkspace("ws3");
                session.logout();
                session = repository.login("ws3");
                session.logout();
                return null;
            }
        }, "config/repo-config-persistent-predefined-ws.json");


        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                JcrSession session = repository.login("default");
                session.logout();
                session = repository.login("ws1");
                session.logout();
                session = repository.login("ws2");
                session.logout();
                session = repository.login("ws3");
                session.logout();
                return null;
            }
        }, "config/repo-config-persistent-predefined-ws-update.json");

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                JcrSession session = repository.login("ws2");
                session.logout();
                return null;
            }
        }, "config/repo-config-persistent-predefined-ws.json");
    }

    @Test
    @FixFor( "MODE-2142" )
    public void shouldAllowChangingNamespacePrefixesInSession() throws Exception {
        FileUtil.delete("target/persistent_repository/");

        String repositoryConfigFile = "config/repo-config-persistent-cache.json";

        final String prefix = "admb";
        final String uri = "http://www.admb.be/modeshape/admb/1.0";
        final String nodeTypeName = prefix + ":test";

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
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

                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                Node node = session.getNode("/testNode");
                assertEquals(nodeTypeName, node.getPrimaryNodeType().getName());
                return null;
            }
        }, repositoryConfigFile);
    }
}
