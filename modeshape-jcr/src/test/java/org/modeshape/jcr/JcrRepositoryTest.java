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

import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import org.infinispan.Cache;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.observation.Event;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.transaction.TransactionManager;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.RepositoryStatistics.DurationActivity;
import org.modeshape.jcr.RepositoryStatistics.History;
import org.modeshape.jcr.RepositoryStatistics.Statistics;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.monitor.Window;

public class JcrRepositoryTest extends AbstractTransactionalTest {

    private Environment environment;
    private RepositoryConfiguration config;
    private JcrRepository repository;
    private JcrSession session;

    @Before
    public void beforeEach() throws Exception {
        environment = new TestingEnvironment();
        config = new RepositoryConfiguration("repoName", environment);
        repository = new JcrRepository(config);
        repository.start();
    }

    @After
    public void afterEach() throws Exception {
        if (session != null) {
            try {
                session.logout();
            } finally {
                session = null;
                try {
                    TestingUtil.killRepositories(repository);
                } finally {
                    repository = null;
                    config = null;
                    environment.shutdown();
                }
            }
        }
    }

    protected TransactionManager getTransactionManager() {
        return repository.transactionManager();
    }

    @Test
    public void shouldCreateRepositoryInstanceWithoutPassingInCacheManager() throws Exception {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        JcrRepository repository = new JcrRepository(config);
        repository.start();
        try {
            Session session = repository.login();
            assertThat(session, is(notNullValue()));
        } finally {
            repository.shutdown().get(3L, TimeUnit.SECONDS);
            JTATestUtil.clearJBossJTADefaultStoreLocation();
        }
    }

    @Test
    public void shouldAllowCreationOfSessionForDefaultWorkspaceWithoutUsingCredentials() throws Exception {
        JcrSession session1 = repository.login();
        assertThat(session1.isLive(), is(true));
    }

    @Test( expected = NoSuchWorkspaceException.class )
    public void shouldNotAllowCreatingSessionForNonExistantWorkspace() throws Exception {
        repository.login("non-existant-workspace");
    }

    @Test
    public void shouldAllowShuttingDownAndRestarting() throws Exception {
        JcrSession session1 = repository.login();
        JcrSession session2 = repository.login();
        assertThat(session1.isLive(), is(true));
        assertThat(session2.isLive(), is(true));
        session2.logout();
        assertThat(session1.isLive(), is(true));
        assertThat(session2.isLive(), is(false));

        repository.shutdown().get(3L, TimeUnit.SECONDS);
        assertThat(session1.isLive(), is(false));
        assertThat(session2.isLive(), is(false));

        repository.start();
        JcrSession session3 = repository.login();
        assertThat(session1.isLive(), is(false));
        assertThat(session2.isLive(), is(false));
        assertThat(session3.isLive(), is(true));
        session3.logout();
    }

    @Test
    public void shouldAllowCreatingNewWorkspacesByDefault() throws Exception {
        // Verify the workspace does not exist yet ...
        try {
            repository.login("new-workspace");
        } catch (NoSuchWorkspaceException e) {
            // expected
        }
        JcrSession session1 = repository.login();
        assertThat(session1.getRootNode(), is(notNullValue()));
        session1.getWorkspace().createWorkspace("new-workspace");

        // Now create a session to that workspace ...
        JcrSession session2 = repository.login("new-workspace");
        assertThat(session2.getRootNode(), is(notNullValue()));
    }

    @Test
    public void shouldAllowDestroyingWorkspacesByDefault() throws Exception {
        // Verify the workspace does not exist yet ...
        try {
            repository.login("new-workspace");
        } catch (NoSuchWorkspaceException e) {
            // expected
        }
        JcrSession session1 = repository.login();
        assertThat(session1.getRootNode(), is(notNullValue()));
        session1.getWorkspace().createWorkspace("new-workspace");

        // Now create a session to that workspace ...
        JcrSession session2 = repository.login("new-workspace");
        assertThat(session2.getRootNode(), is(notNullValue()));
    }

    @Test
    public void shouldReturnNullForNullDescriptorKey() {
        assertThat(repository.getDescriptor(null), is(nullValue()));
    }

    @Test
    public void shouldReturnNullForEmptyDescriptorKey() {
        assertThat(repository.getDescriptor(""), is(nullValue()));
    }

    @Test
    public void shouldProvideBuiltInDescriptorKeys() {
        testDescriptorKeys(repository);
    }

    @Test
    public void shouldProvideDescriptorValues() {
        testDescriptorValues(repository);
    }

    @Test
    public void shouldProvideBuiltInDescriptorsWhenNotSuppliedDescriptors() throws Exception {
        testDescriptorKeys(repository);
        testDescriptorValues(repository);
    }

    @Test
    public void shouldProvideRepositoryWorkspaceNamesDescriptor() throws ValueFormatException {
        Set<String> workspaceNames = repository.repositoryCache().getWorkspaceNames();
        Set<String> descriptorValues = new HashSet<String>();
        for (JcrValue value : repository.getDescriptorValues(org.modeshape.jcr.api.Repository.REPOSITORY_WORKSPACES)) {
            descriptorValues.add(value.getString());
        }
        assertThat(descriptorValues, is(workspaceNames));
    }

    /**
     * Skipping this test because it purposefully runs over 6 seconds, mostly just waiting for the statistics thread to wake up
     * once every 5 seconds.
     * 
     * @throws Exception
     */
    @Ignore
    @Test
    public void shouldProvideStatistics() throws Exception {
        for (int i = 0; i != 3; ++i) {
            JcrSession session1 = repository.login();
            assertThat(session1.getRootNode(), is(notNullValue()));
        }
        // wait for 6 seconds, so that the statistics have a value ...
        Thread.sleep(6000L);
        History history = repository.getRepositoryStatistics().getHistory(ValueMetric.SESSION_COUNT, Window.PREVIOUS_60_SECONDS);
        Statistics[] stats = history.getStats();
        assertThat(stats.length, is(12));
        assertThat(stats[0], is(nullValue()));
        assertThat(stats[11], is(notNullValue()));
        assertThat(stats[11].getMaximum(), is(3L));
        assertThat(stats[11].getMinimum(), is(3L));
        assertThat(history.getTotalDuration(TimeUnit.SECONDS), is(60L));
        System.out.println(history);
    }

    /**
     * Skipping this test because it purposefully runs over 18 seconds, mostly just waiting for the statistics thread to wake up
     * once every 5 seconds.
     * 
     * @throws Exception
     */
    @Ignore
    @Test
    public void shouldProvideStatisticsForMultipleSeconds() throws Exception {
        LinkedList<JcrSession> sessions = new LinkedList<JcrSession>();
        for (int i = 0; i != 5; ++i) {
            JcrSession session1 = repository.login();
            assertThat(session1.getRootNode(), is(notNullValue()));
            sessions.addFirst(session1);
            Thread.sleep(1000L);
        }
        Thread.sleep(6000L);
        while (sessions.peek() != null) {
            JcrSession session = sessions.poll();
            session.logout();
            Thread.sleep(1000L);
        }
        History history = repository.getRepositoryStatistics().getHistory(ValueMetric.SESSION_COUNT, Window.PREVIOUS_60_SECONDS);
        Statistics[] stats = history.getStats();
        assertThat(stats.length, is(12));
        assertThat(history.getTotalDuration(TimeUnit.SECONDS), is(60L));
        System.out.println(history);

        DurationActivity[] lifetimes = repository.getRepositoryStatistics().getLongestRunning(DurationMetric.SESSION_LIFETIME);
        System.out.println("Session lifetimes: ");
        for (DurationActivity activity : lifetimes) {
            System.out.println("  " + activity);
        }
    }

    @SuppressWarnings( "deprecation" )
    private void testDescriptorKeys( Repository repository ) {
        String[] keys = repository.getDescriptorKeys();
        assertThat(keys, notNullValue());
        assertThat(keys.length >= 15, is(true));
        assertThat(keys, hasItemInArray(Repository.LEVEL_1_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.LEVEL_2_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_LOCKING_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_OBSERVATION_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_QUERY_SQL_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_TRANSACTIONS_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_VERSIONING_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.QUERY_XPATH_DOC_ORDER));
        assertThat(keys, hasItemInArray(Repository.QUERY_XPATH_POS_INDEX));
        assertThat(keys, hasItemInArray(Repository.REP_NAME_DESC));
        assertThat(keys, hasItemInArray(Repository.REP_VENDOR_DESC));
        assertThat(keys, hasItemInArray(Repository.REP_VENDOR_URL_DESC));
        assertThat(keys, hasItemInArray(Repository.REP_VERSION_DESC));
        assertThat(keys, hasItemInArray(Repository.SPEC_NAME_DESC));
        assertThat(keys, hasItemInArray(Repository.SPEC_VERSION_DESC));
    }

    @SuppressWarnings( "deprecation" )
    private void testDescriptorValues( Repository repository ) {
        assertThat(repository.getDescriptor(Repository.LEVEL_1_SUPPORTED), is("true"));
        assertThat(repository.getDescriptor(Repository.LEVEL_2_SUPPORTED), is("true"));
        assertThat(repository.getDescriptor(Repository.OPTION_LOCKING_SUPPORTED), is("true"));
        assertThat(repository.getDescriptor(Repository.OPTION_OBSERVATION_SUPPORTED), is("true"));
        assertThat(repository.getDescriptor(Repository.OPTION_QUERY_SQL_SUPPORTED), is("true"));
        assertThat(repository.getDescriptor(Repository.OPTION_TRANSACTIONS_SUPPORTED), is("false"));
        assertThat(repository.getDescriptor(Repository.OPTION_VERSIONING_SUPPORTED), is("true"));
        assertThat(repository.getDescriptor(Repository.QUERY_XPATH_DOC_ORDER), is("false"));
        assertThat(repository.getDescriptor(Repository.QUERY_XPATH_POS_INDEX), is("true"));
        assertThat(repository.getDescriptor(Repository.REP_NAME_DESC), is("ModeShape"));
        assertThat(repository.getDescriptor(Repository.REP_VENDOR_DESC), is("JBoss, a division of Red Hat"));
        assertThat(repository.getDescriptor(Repository.REP_VENDOR_URL_DESC), is("http://www.modeshape.org"));
        assertThat(repository.getDescriptor(Repository.REP_VERSION_DESC), is(notNullValue()));
        assertThat(repository.getDescriptor(Repository.REP_VERSION_DESC).startsWith("3."), is(true));
        assertThat(repository.getDescriptor(Repository.SPEC_NAME_DESC), is(JcrI18n.SPEC_NAME_DESC.text()));
        assertThat(repository.getDescriptor(Repository.SPEC_VERSION_DESC), is("2.0"));
    }

    @Test
    public void shouldReturnNullWhenDescriptorKeyIsNull() {
        assertThat(repository.getDescriptor(null), is(nullValue()));
    }

    @Test
    public void shouldNotAllowEmptyDescriptorKey() {
        assertThat(repository.getDescriptor(""), is(nullValue()));
    }

    @Test
    public void shouldNotProvideRepositoryWorkspaceNamesDescriptorIfOptionSetToFalse() throws Exception {
        assertThat(repository.getDescriptor(org.modeshape.jcr.api.Repository.REPOSITORY_WORKSPACES), is(nullValue()));
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldHaveRootNode() throws Exception {
        session = createSession();
        javax.jcr.Node root = session.getRootNode();
        String uuid = root.getIdentifier();

        // Should be referenceable ...
        assertThat(root.isNodeType("mix:referenceable"), is(true));

        // Should have a UUID ...
        assertThat(root.getUUID(), is(uuid));

        // Should have an identifier ...
        assertThat(root.getIdentifier(), is(uuid));

        // Get the children of the root node ...
        javax.jcr.NodeIterator iter = root.getNodes();
        javax.jcr.Node system = iter.nextNode();
        assertThat(system.getName(), is("jcr:system"));

        // Add a child node ...
        javax.jcr.Node childA = root.addNode("childA", "nt:unstructured");
        assertThat(childA, is(notNullValue()));
        iter = root.getNodes();
        javax.jcr.Node system2 = iter.nextNode();
        javax.jcr.Node childA2 = iter.nextNode();
        assertThat(system2.getName(), is("jcr:system"));
        assertThat(childA2.getName(), is("childA"));
    }

    @Test
    public void shouldHaveSystemBranch() throws Exception {
        session = createSession();
        javax.jcr.Node root = session.getRootNode();
        AbstractJcrNode system = (AbstractJcrNode)root.getNode("jcr:system");
        assertThat(system, is(notNullValue()));
    }

    @Test
    public void shouldHaveRegisteredModeShapeSpecificNamespacesNamespaces() throws Exception {
        session = createSession();
        // Don't use the constants, since this needs to check that the actual values are correct
        assertThat(session.getNamespaceURI("mode"), is("http://www.modeshape.org/1.0"));
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotHaveModeShapeInternalNamespaceFromVersion2() throws Exception {
        session = createSession();
        // Don't use the constants, since this needs to check that the actual values are correct
        session.getNamespaceURI("modeint");
    }

    @Test
    public void shouldHaveRegisteredThoseNamespacesDefinedByTheJcrSpecification() throws Exception {
        session = createSession();
        // Don't use the constants, since this needs to check that the actual values are correct
        assertThat(session.getNamespaceURI("mode"), is("http://www.modeshape.org/1.0"));
        assertThat(session.getNamespaceURI("jcr"), is("http://www.jcp.org/jcr/1.0"));
        assertThat(session.getNamespaceURI("mix"), is("http://www.jcp.org/jcr/mix/1.0"));
        assertThat(session.getNamespaceURI("nt"), is("http://www.jcp.org/jcr/nt/1.0"));
        assertThat(session.getNamespaceURI(""), is(""));
    }

    @Test
    public void shouldHaveRegisteredThoseNamespacesDefinedByTheJcrApiJavaDoc() throws Exception {
        session = createSession();
        // Don't use the constants, since this needs to check that the actual values are correct
        assertThat(session.getNamespaceURI("sv"), is("http://www.jcp.org/jcr/sv/1.0"));
        assertThat(session.getNamespaceURI("xmlns"), is("http://www.w3.org/2000/xmlns/"));
    }

    protected JcrSession createSession() throws Exception {
        return repository.login();
    }

    protected JcrSession createSession( final String workspace ) throws Exception {
        return repository.login(workspace);
    }

    @Ignore( "GC behavior is non-deterministic from the application's POV - this test _will_ occasionally fail" )
    @Test
    public void shouldAllowManySessionLoginsAndLogouts() throws Exception {
        Session session = null;
        for (int i = 0; i < 10000; i++) {
            session = repository.login();
            session.logout();
        }

        session = repository.login();
        session = null;

        // Give the gc a chance to run
        System.gc();
        Thread.sleep(100);

        assertThat(repository.runningState().activeSessinCount(), is(0));
    }

    @Ignore( "This test normally sleeps for 30 seconds" )
    @Test
    public void shouldCleanUpLocksFromDeadSessions() throws Exception {
        String lockedNodeName = "lockedNode";
        JcrSession locker = repository.login();

        // Create a node to lock
        javax.jcr.Node lockedNode = locker.getRootNode().addNode(lockedNodeName);
        lockedNode.addMixin("mix:lockable");
        locker.save();

        // Create a session-scoped lock (not deep)
        locker.getWorkspace().getLockManager().lock(lockedNode.getPath(), false, true, 1L, "me");
        assertThat(lockedNode.isLocked(), is(true));

        Session reader = repository.login();
        javax.jcr.Node readerNode = (javax.jcr.Node)reader.getItem("/" + lockedNodeName);
        assertThat(readerNode.isLocked(), is(true));

        // No locks should have changed yet.
        repository.runningState().cleanUpLocks();
        assertThat(lockedNode.isLocked(), is(true));
        assertThat(readerNode.isLocked(), is(true));

        /*       
         * Simulate the GC cleaning up the session and it being purged from the activeSessions() map.
         * This can't really be tested in a consistent way due to a lack of specificity around when
         * the garbage collector runs. The @Ignored test above does cause a GC sweep on by computer and
         * confirms that the code works in principle. A different chicken dance may be required to
         * fully test this on a different computer.
         */
        repository.runningState().removeSession(locker);
        Thread.sleep(RepositoryConfiguration.LOCK_EXTENSION_INTERVAL_IN_MILLIS + 100);

        // The locker thread should be inactive and the lock cleaned up
        repository.runningState().cleanUpLocks();
        assertThat(readerNode.isLocked(), is(false));
    }

    @Test
    public void shouldAllowCreatingWorkspaces() throws Exception {
        RepositoryConfiguration config = null;
        config = RepositoryConfiguration.read("{ \"name\" : \"repoName\", \"workspaces\" : { \"allowCreation\" : true } }");
        config = new RepositoryConfiguration(config.getDocument(), "repoName", environment);
        repository = new JcrRepository(config);
        repository.start();

        // Create several sessions ...
        Session session2 = null;
        Session session3 = null;
        try {
            session = createSession();
            session2 = createSession();

            // Create a new workspace ...
            String newWorkspaceName = "MyCarWorkspace";
            session.getWorkspace().createWorkspace(newWorkspaceName);
            assertAccessibleWorkspace(session, newWorkspaceName);
            assertAccessibleWorkspace(session2, newWorkspaceName);
            session.logout();

            session3 = createSession();
            assertAccessibleWorkspace(session2, newWorkspaceName);
            assertAccessibleWorkspace(session3, newWorkspaceName);

            // Create a session for this new workspace ...
            session = createSession(newWorkspaceName);
        } finally {
            try {
                if (session2 != null) session2.logout();
            } finally {
                if (session3 != null) session3.logout();
            }
        }

    }

    protected void assertAccessibleWorkspace( Session session,
                                              String workspaceName ) throws Exception {
        assertContains(session.getWorkspace().getAccessibleWorkspaceNames(), workspaceName);
    }

    protected void assertContains( String[] actuals,
                                   String... expected ) {
        // Each expected must appear in the actuals ...
        for (String expect : expected) {
            if (expect == null) continue;
            boolean found = false;
            for (String actual : actuals) {
                if (expect.equals(actual)) {
                    found = true;
                    break;
                }
            }
            assertThat("Did not find '" + expect + "' in the actuals: " + actuals, found, is(true));
        }
    }

    @Test
    @FixFor( "MODE-1269" )
    public void shouldAllowReindexingEntireWorkspace() throws Exception {
        session = createSession();
        session.getWorkspace().reindex();
    }

    @Test
    @FixFor( "MODE-1269" )
    public void shouldAllowReindexingSubsetOfWorkspace() throws Exception {
        session = createSession();
        session.getWorkspace().reindex("/");
    }

    @Test
    @FixFor( "MODE-1269" )
    public void shouldAllowAsynchronousReindexingEntireWorkspace() throws Exception {
        session = createSession();
        Future<Boolean> future = session.getWorkspace().reindexAsync();
        assertThat(future, is(notNullValue()));
        assertThat(future.get(), is(true)); // get() blocks until done
    }

    @Test
    @FixFor( "MODE-1269" )
    public void shouldAllowAsynchronousReindexingSubsetOfWorkspace() throws Exception {
        session = createSession();
        Future<Boolean> future = session.getWorkspace().reindexAsync("/");
        assertThat(future, is(notNullValue()));
        assertThat(future.get(), is(true)); // get() blocks until done
    }

    @FixFor( "MODE-1498" )
    @Test
    public void shouldWorkWithUserDefinedTransactions() throws Exception {
        session = createSession();

        Session session2 = createSession();
        try {

            // Create a listener to count the changes ...
            SimpleListener listener = addListener(3); // we'll create 3 nodes ...
            assertThat(listener.getActualEventCount(), is(0));

            // Check that the node is not visible to the other session ...
            session.refresh(false);
            nodeDoesNotExist(session2, "/", "txnNode1");
            nodeDoesNotExist(session2, "/", "txnNode2");

            // Start a transaction ...
            final TransactionManager txnMgr = getTransactionManager();
            txnMgr.begin();
            assertThat(listener.getActualEventCount(), is(0));
            Node txnNode1 = session.getRootNode().addNode("txnNode1");
            Node txnNode1a = txnNode1.addNode("txnNodeA");
            assertThat(txnNode1a, is(notNullValue()));
            session.save();
            assertThat(listener.getActualEventCount(), is(0));

            // Check that the node is not visible to the other session ...
            nodeDoesNotExist(session2, "/", "txnNode1");
            nodeDoesNotExist(session2, "/", "txnNode2");
            session2.refresh(false);
            nodeDoesNotExist(session2, "/", "txnNode1");
            nodeDoesNotExist(session2, "/", "txnNode2");

            // sleep a bit to let any incorrect events propagate through the system ...
            Thread.sleep(100L);
            assertThat(listener.getActualEventCount(), is(0));

            Node txnNode2 = session.getRootNode().addNode("txnNode2");
            assertThat(txnNode2, is(notNullValue()));
            session.save();
            assertThat(listener.getActualEventCount(), is(0));
            assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
            assertThat(session.getRootNode().hasNode("txnNode2"), is(true));

            // Check that the node is not visible to the other session ...
            nodeDoesNotExist(session2, "/", "txnNode1");
            nodeDoesNotExist(session2, "/", "txnNode2");
            session2.refresh(false);
            nodeDoesNotExist(session2, "/", "txnNode1");
            nodeDoesNotExist(session2, "/", "txnNode2");

            // sleep a bit to let any incorrect events propagate through the system ...
            Thread.sleep(100L);
            assertThat(listener.getActualEventCount(), is(0));

            assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
            assertThat(session.getRootNode().hasNode("txnNode2"), is(true));

            // Now commit the transaction ...
            txnMgr.commit();
            listener.waitForEvents();

            nodeExists(session, "/", "txnNode1");
            nodeExists(session, "/", "txnNode2");

            // Check that the node IS visible to the other session ...
            nodeExists(session2, "/", "txnNode1");
            nodeExists(session2, "/", "txnNode2");
            session2.refresh(false);
            nodeExists(session2, "/", "txnNode1");
            nodeExists(session2, "/", "txnNode2");

        } finally {
            session2.logout();
        }
    }

    @FixFor( "MODE-1498" )
    @Test
    public void shouldWorkWithUserDefinedTransactionsThatUseRollback() throws Exception {
        session = createSession();

        Session session2 = createSession();
        try {

            // Create a listener to count the changes ...
            SimpleListener listener = addListener(3); // we'll create 3 nodes ...
            assertThat(listener.getActualEventCount(), is(0));

            // Check that the node is not visible to the other session ...
            session.refresh(false);
            nodeDoesNotExist(session2, "/", "txnNode1");
            nodeDoesNotExist(session2, "/", "txnNode2");

            // Start a transaction ...
            final TransactionManager txnMgr = getTransactionManager();
            txnMgr.begin();
            assertThat(listener.getActualEventCount(), is(0));
            Node txnNode1 = session.getRootNode().addNode("txnNode1");
            Node txnNode1a = txnNode1.addNode("txnNodeA");
            assertThat(txnNode1a, is(notNullValue()));
            session.save();
            assertThat(listener.getActualEventCount(), is(0));

            // Check that the node is not visible to the other session ...
            nodeDoesNotExist(session2, "/", "txnNode1");
            nodeDoesNotExist(session2, "/", "txnNode2");
            session2.refresh(false);
            nodeDoesNotExist(session2, "/", "txnNode1");
            nodeDoesNotExist(session2, "/", "txnNode2");

            // sleep a bit to let any incorrect events propagate through the system ...
            Thread.sleep(100L);
            assertThat(listener.getActualEventCount(), is(0));

            Node txnNode2 = session.getRootNode().addNode("txnNode2");
            assertThat(txnNode2, is(notNullValue()));
            session.save();
            assertThat(listener.getActualEventCount(), is(0));
            assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
            assertThat(session.getRootNode().hasNode("txnNode2"), is(true));

            // Check that the node is not visible to the other session ...
            nodeDoesNotExist(session2, "/", "txnNode1");
            nodeDoesNotExist(session2, "/", "txnNode2");
            session2.refresh(false);
            nodeDoesNotExist(session2, "/", "txnNode1");
            nodeDoesNotExist(session2, "/", "txnNode2");

            // sleep a bit to let any incorrect events propagate through the system ...
            Thread.sleep(100L);
            assertThat(listener.getActualEventCount(), is(0));

            assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
            assertThat(session.getRootNode().hasNode("txnNode2"), is(true));

            // Now commit the transaction ...
            txnMgr.rollback();

            // There should have been no events ...
            Thread.sleep(100L);
            assertThat(listener.getActualEventCount(), is(0));

            // The nodes still exist in the session, since 'refresh' hasn't been called ...
            assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
            assertThat(session.getRootNode().hasNode("txnNode2"), is(true));
            session.refresh(false);
            assertThat(session.getRootNode().hasNode("txnNode1"), is(false));
            assertThat(session.getRootNode().hasNode("txnNode2"), is(false));

            // Check that the node IS NOT visible to the other session ...
            session2.refresh(false);
            nodeDoesNotExist(session2, "/", "txnNode1");
            nodeDoesNotExist(session2, "/", "txnNode2");

        } finally {
            session2.logout();
        }
    }

    @Test
    @FixFor("MODE-1526")
    @Ignore("Ignored because it crashes")
    public void shouldKeepPersistentDataAcrossRestart() throws Exception {
        File contentFolder = new File("target/persistent_repository/store/persistentRepository");
        boolean testNodeShouldExist = contentFolder.exists() && contentFolder.isDirectory();

        URL config = getClass().getClassLoader().getResource("config/repo-config-persistent-cache.json");
        JcrRepository repository = new JcrRepository(RepositoryConfiguration.read(config));
        repository.start();


        try {
            Session session = repository.login();
            if (testNodeShouldExist) {
                assertNotNull(session.getNode("/testNode"));
            }
            else {
                session.getRootNode().addNode("testNode");
                session.save();
            }

            for (Cache cache : repository.caches()) {
               cache.stop();
            }
            repository.shutdown();

            repository.start();
            for (Cache cache : repository.caches()) {
                cache.start();
            }

            session = repository.login();
            assertNotNull(session.getNode("/testNode"));
        } finally {
            TestingUtil.killRepository(repository);
        }
    }

    protected void nodeExists( Session session,
                               String parentPath,
                               String childName,
                               boolean exists ) throws Exception {
        Node parent = session.getNode(parentPath);
        assertThat(parent.hasNode(childName), is(exists));
        String path = parent.getPath();
        if (parent.getDepth() != 0) path = path + "/";
        path = path + childName;

        String sql = "SELECT * FROM [nt:base] WHERE PATH() = '" + path + "'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        assertThat(result.getNodes().getSize(), is(exists ? 1L : 0L));
    }

    protected void nodeExists( Session session,
                               String parentPath,
                               String childName ) throws Exception {
        nodeExists(session, parentPath, childName, true);
    }

    protected void nodeDoesNotExist( Session session,
                                     String parentPath,
                                     String childName ) throws Exception {
        nodeExists(session, parentPath, childName, false);
    }

    private static final int ALL_EVENTS = Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED
                                          | Event.PROPERTY_REMOVED;

    SimpleListener addListener( int expectedEventsCount ) throws Exception {
        return addListener(expectedEventsCount, ALL_EVENTS, null, false, null, null, false);
    }

    SimpleListener addListener( int expectedEventsCount,
                                int eventTypes,
                                String absPath,
                                boolean isDeep,
                                String[] uuids,
                                String[] nodeTypeNames,
                                boolean noLocal ) throws Exception {
        return addListener(expectedEventsCount, 1, eventTypes, absPath, isDeep, uuids, nodeTypeNames, noLocal);
    }

    SimpleListener addListener( int expectedEventsCount,
                                int numIterators,
                                int eventTypes,
                                String absPath,
                                boolean isDeep,
                                String[] uuids,
                                String[] nodeTypeNames,
                                boolean noLocal ) throws Exception {
        return addListener(this.session,
                           expectedEventsCount,
                           numIterators,
                           eventTypes,
                           absPath,
                           isDeep,
                           uuids,
                           nodeTypeNames,
                           noLocal);
    }

    SimpleListener addListener( Session session,
                                int expectedEventsCount,
                                int numIterators,
                                int eventTypes,
                                String absPath,
                                boolean isDeep,
                                String[] uuids,
                                String[] nodeTypeNames,
                                boolean noLocal ) throws Exception {
        SimpleListener listener = new SimpleListener(expectedEventsCount, numIterators, eventTypes);
        session.getWorkspace()
               .getObservationManager()
               .addEventListener(listener, eventTypes, absPath, isDeep, uuids, nodeTypeNames, noLocal);
        return listener;
    }

}
