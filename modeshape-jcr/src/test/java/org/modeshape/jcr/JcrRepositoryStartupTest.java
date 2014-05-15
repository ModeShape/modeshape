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
import org.modeshape.connector.mock.MockConnector;
import org.modeshape.jcr.api.Workspace;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.document.DocumentStore;
import org.modeshape.jcr.security.SimplePrincipal;
import org.modeshape.jcr.value.Property;
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
        FileUtil.delete("target/persistent_repository");

        final String workspace1 = "ws1";
        final String workspace2 = "ws2";

        String repositoryConfigFile = "config/repo-config-persistent-cache.json";

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();

                session.getRootNode().addNode("testNode");
                session.save();

                // create 2 new workspaces
                session.getWorkspace().createWorkspace(workspace1);
                session.getWorkspace().createWorkspace(workspace2);
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
                Session newWsSession = repository.login(workspace1);
                newWsSession.getRootNode().addNode("newWsTestNode");
                newWsSession.save();
                newWsSession.logout();

                Session newWs1Session = repository.login(workspace2);
                newWs1Session.getWorkspace().deleteWorkspace(workspace2);
                newWs1Session.logout();

                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session newWsSession = repository.login(workspace1);
                assertNotNull(newWsSession.getNode("/newWsTestNode"));
                newWsSession.logout();

                // check a workspace was deleted
                try {
                    repository.login(workspace2);
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
                changeLastUpgradeId(repository, Upgrades.ModeShape_3_6_0.INSTANCE.getId() - 1);

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

    @Test
    @FixFor("MODE-2167")
    public void shouldDisableACLsIfAllPoliciesAreRemoved() throws Exception {
        FileUtil.delete("target/persistent_repository/");

        String repositoryConfigFile = "config/repo-config-persistent-cache.json";

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();
                Node testNode = session.getRootNode().addNode("testNode");
                testNode.addNode("node1");
                testNode.addNode("node2");
                session.save();

                AccessControlManager acm = session.getAccessControlManager();

                AccessControlList aclNode1 = getACL(acm, "/testNode/node1");
                aclNode1.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                                          new Privilege[] { acm.privilegeFromName(Privilege.JCR_ALL) });
                acm.setPolicy("/testNode/node1", aclNode1);

                AccessControlList aclNode2 = getACL(acm, "/testNode/node2");
                aclNode2.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                                          new Privilege[] { acm.privilegeFromName(Privilege.JCR_ALL) });
                acm.setPolicy("/testNode/node2", aclNode2);

                //access control should not be enabled yet because we haven't saved the session
                assertFalse(repository.runningState().repositoryCache().isAccessControlEnabled());

                session.save();
                assertTrue(repository.runningState().repositoryCache().isAccessControlEnabled());

                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                assertTrue(repository.runningState().repositoryCache().isAccessControlEnabled());

                Session session = repository.login();
                AccessControlManager acm = session.getAccessControlManager();
                //TODO author=Horia Chiorean date=25-Mar-14 description=Why null here !?!
                acm.removePolicy("/testNode/node1", null);
                acm.removePolicy("/testNode/node2", null);
                session.save();

                assertFalse(repository.runningState().repositoryCache().isAccessControlEnabled());

                session.getNode("/testNode").remove();
                session.save();
                return null;
            }
        }, repositoryConfigFile);
    }

    @Test
    @FixFor("MODE-2167")
    public void shouldRun3_7_4UpgradeFunction() throws Exception {
        FileUtil.delete("target/persistent_repository/");
        String config = "config/repo-config-persistent-indexes-disk.json";
        //first run is empty, so no upgrades will be performed
        startRunStop(new RepositoryOperation() {
            @SuppressWarnings( "deprecation" )
            @Override
            public Void call() throws Exception {
                changeLastUpgradeId(repository, Upgrades.ModeShape_3_7_4.INSTANCE.getId() - 1);


                //modify some ACLs
                JcrSession session = repository.login();
                session.getRootNode().addNode("testNode");
                session.save();

                AccessControlManager acm = session.getAccessControlManager();

                AccessControlList acl = getACL(acm, "/testNode");
                acl.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                                          new Privilege[] { acm.privilegeFromName(Privilege.JCR_ALL) });
                acm.setPolicy("/testNode", acl);
                session.save();

                //remove the new property from 4.0 which actually stores the ACL count to simulate a pre 4.0 repository
                SessionCache systemSession = repository.createSystemSession(repository.runningState().context(), false);
                SystemContent systemContent = new SystemContent(systemSession);
                MutableCachedNode systemNode = systemContent.mutableSystemNode();
                systemNode.removeProperty(systemSession, ModeShapeLexicon.ACL_COUNT);
                systemSession.save();
                return null;
            }
        }, config);

        //second run should run the upgrade
        startRunStop(new RepositoryOperation() {
            @SuppressWarnings( "deprecation" )
            @Override
            public Void call() throws Exception {
                //check that the upgrade function correctly added the new property
                SessionCache systemSession = repository.createSystemSession(repository.runningState().context(), false);
                SystemContent systemContent = new SystemContent(systemSession);
                MutableCachedNode systemNode = systemContent.mutableSystemNode();
                Property aclCountProp = systemNode.getProperty(ModeShapeLexicon.ACL_COUNT, systemSession);
                assertNotNull("ACL count property not found after upgrade", aclCountProp);
                assertEquals(1, Long.valueOf(aclCountProp.getFirstValue().toString()).longValue());

                //force a 2nd upgrade
                changeLastUpgradeId(repository, Upgrades.ModeShape_3_7_4.INSTANCE.getId() - 1);

                //remove all ACLs
                JcrSession session = repository.login();
                AccessControlManager acm = session.getAccessControlManager();
                //TODO author=Horia Chiorean date=25-Mar-14 description=Why null ?!
                acm.removePolicy("/testNode", null);
                session.save();

                //remove the new property from 4.0 which actually stores the ACL count to simulate a pre 4.0 repository
                systemNode.removeProperty(systemSession, ModeShapeLexicon.ACL_COUNT);
                systemSession.save();
                return null;
            }
        }, config);

        //check that the upgrade disabled ACLs
        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {

                SessionCache systemSession = repository.createSystemSession(repository.runningState().context(), true);
                SystemContent systemContent = new SystemContent(systemSession);
                CachedNode systemNode = systemContent.systemNode();
                Property aclCountProp = systemNode.getProperty(ModeShapeLexicon.ACL_COUNT, systemSession);
                assertNotNull("ACL count property not found after upgrade", aclCountProp);
                assertEquals(0, Long.valueOf(aclCountProp.getFirstValue().toString()).longValue());

                assertFalse(repository.runningState().repositoryCache().isAccessControlEnabled());
                return null;
            }
        }, config);
    }

    private void changeLastUpgradeId( JcrRepository repository, int value ) {
        //modify the repository-info document to force an upgrade on the next restart
        DocumentStore documentStore =  repository.documentStore();
        EditableDocument editableDocument = documentStore.localStore().get("repository:info").editDocumentContent();
        editableDocument.set("lastUpgradeId", value);
        documentStore.localStore().put("repository:info", editableDocument);
    }

    private AccessControlList getACL(  AccessControlManager acm, String absPath ) throws Exception {
        AccessControlPolicyIterator it = acm.getApplicablePolicies(absPath);
        if (it.hasNext()) {
            return (AccessControlList)it.nextAccessControlPolicy();
        }
        return (AccessControlList)acm.getPolicies(absPath)[0];
    }

}
