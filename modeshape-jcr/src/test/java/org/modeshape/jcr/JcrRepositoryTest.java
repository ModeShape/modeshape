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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.observation.Event;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import javax.transaction.TransactionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.RepositoryStatistics.MetricHistory;
import org.modeshape.jcr.api.monitor.DurationActivity;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.History;
import org.modeshape.jcr.api.monitor.Statistics;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.monitor.Window;
import org.modeshape.jcr.security.SimplePrincipal;

public class JcrRepositoryTest extends AbstractTransactionalTest {

    private Environment environment;
    private RepositoryConfiguration config;
    private JcrRepository repository;
    private JcrSession session;
    protected boolean print = false;

    @Before
    public void beforeEach() throws Exception {
        FileUtil.delete("target/persistent_repository");

        environment = new TestingEnvironment();
        config = new RepositoryConfiguration("repoName", environment);
        repository = new JcrRepository(config);
        repository.start();
        print = false;
    }

    @After
    public void afterEach() throws Exception {
        if (session != null) {
            try {
                session.logout();
            } finally {
                session = null;
            }
        }
        shutdownDefaultRepository();
        environment.shutdown();
    }

    private void shutdownDefaultRepository() {
        if (repository != null) {
            try {
                TestingUtil.killRepositories(repository);
            } finally {
                repository = null;
                config = null;
            }
        }
    }

    protected TransactionManager getTransactionManager() {
        return repository.transactionManager();
    }

    @Test
    public void shouldCreateRepositoryInstanceWithoutPassingInCacheManager() throws Exception {
        shutdownDefaultRepository();
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        repository = new JcrRepository(config);
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

    @FixFor( {"MODE-1834", "MODE-2004"} )
    @Test
    public void shouldAllowCreatingNewWorkspacesByDefaultWhenUsingTransactionManagerWithOptimisticLocking() throws Exception {
        shutdownDefaultRepository();

        RepositoryConfiguration config = RepositoryConfiguration.read("config/repo-config-filesystem-jbosstxn-optimistic.json");
        repository = new JcrRepository(config);
        repository.start();

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

        // Shut down the repository ...
        assertThat(repository.shutdown().get(), is(true));

        // Start up the repository again, this time by reading the persisted data ...
        repository = new JcrRepository(config);
        repository.start();

        // And verify that the workspace existance was persisted properly ...
        repository.login("new-workspace");
    }

    @FixFor( {"MODE-1834", "MODE-2004"} )
    @Test
    public void shouldAllowCreatingNewWorkspacesByDefaultWhenUsingTransactionManagerWithPessimisticLocking() throws Exception {
        shutdownDefaultRepository();

        RepositoryConfiguration config = RepositoryConfiguration.read("config/repo-config-filesystem-jbosstxn-pessimistic.json");
        repository = new JcrRepository(config);
        repository.start();

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

        // Shut down the repository ...
        assertThat(repository.shutdown().get(), is(true));

        // Start up the repository again, this time by reading the persisted data ...
        repository = new JcrRepository(config);
        repository.start();

        // And verify that the workspace existance was persisted properly ...
        repository.login("new-workspace");
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

    @Test
    public void shouldProvideStatisticsImmediatelyAfterStartup() throws Exception {
        History history = repository.getRepositoryStatistics()
                                    .getHistory(ValueMetric.WORKSPACE_COUNT, Window.PREVIOUS_60_SECONDS);
        Statistics[] stats = history.getStats();
        assertThat(stats.length, is(not(0)));
        assertThat(history.getTotalDuration(TimeUnit.SECONDS), is(60L));
        System.out.println(history);
    }

    /**
     * Skipping this test because it purposefully runs over 60 minutes (!!!), mostly just waiting for the statistics thread to
     * wake up once every 5 seconds.
     * 
     * @throws Exception
     */
    @Ignore
    @Test
    public void shouldProvideStatisticsForAVeryLongTime() throws Exception {
        final AtomicBoolean stop = new AtomicBoolean(false);
        final JcrRepository repository = this.repository;
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                JcrSession[] openSessions = new JcrSession[100 * 6];
                int index = 0;
                while (!stop.get()) {
                    try {
                        for (int i = 0; i != 6; ++i) {
                            JcrSession session1 = repository.login();
                            assertThat(session1.getRootNode(), is(notNullValue()));
                            openSessions[index++] = session1;
                        }
                        if (index >= openSessions.length) {
                            for (int i = 0; i != openSessions.length; ++i) {
                                openSessions[i].logout();
                                openSessions[i] = null;
                            }
                            index = 0;
                        }
                        Thread.sleep(MILLISECONDS.convert(3, SECONDS));
                    } catch (Throwable t) {
                        t.printStackTrace();
                        stop.set(true);
                        break;
                    }
                }
            }
        });
        worker.start();
        // Status thread ...
        final Stopwatch sw = new Stopwatch();
        Thread status = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Starting ...");
                sw.start();
                int counter = 0;
                while (!stop.get()) {
                    try {
                        Thread.sleep(MILLISECONDS.convert(10, SECONDS));
                        if (!stop.get()) {
                            ++counter;
                            sw.lap();
                            System.out.println("   continuing after " + sw.getTotalDuration().toSimpleString());
                        }
                        if (counter % 24 == 0) {
                            History history = repository.getRepositoryStatistics().getHistory(ValueMetric.SESSION_COUNT,
                                                                                              Window.PREVIOUS_60_SECONDS);
                            System.out.println(history);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                        stop.set(true);
                        break;
                    }
                }
            }
        });
        status.start();

        // wait for 65 minutes, so that the statistics have a value ...
        Thread.sleep(MILLISECONDS.convert(65, MINUTES));
        stop.set(true);
        System.out.println();
        Thread.sleep(MILLISECONDS.convert(5, SECONDS));

        History history = repository.getRepositoryStatistics().getHistory(ValueMetric.SESSION_COUNT, Window.PREVIOUS_60_MINUTES);
        Statistics[] stats = history.getStats();
        System.out.println(history);
        assertThat(stats.length, is(MetricHistory.MAX_MINUTES));
        assertThat(stats[0], is(notNullValue()));
        assertThat(stats[11], is(notNullValue()));
        assertThat(stats[59], is(notNullValue()));
        assertThat(history.getTotalDuration(TimeUnit.MINUTES), is(60L));

        history = repository.getRepositoryStatistics().getHistory(ValueMetric.SESSION_COUNT, Window.PREVIOUS_60_SECONDS);
        stats = history.getStats();
        System.out.println(history);
        assertThat(stats.length, is(MetricHistory.MAX_SECONDS));
        assertThat(stats[0], is(notNullValue()));
        assertThat(stats[11], is(notNullValue()));
        assertThat(history.getTotalDuration(TimeUnit.SECONDS), is(60L));

        history = repository.getRepositoryStatistics().getHistory(ValueMetric.SESSION_COUNT, Window.PREVIOUS_24_HOURS);
        stats = history.getStats();
        System.out.println(history);
        assertThat(stats.length, is(not(0)));
        assertThat(stats[0], is(nullValue()));
        assertThat(stats[23], is(notNullValue()));
        assertThat(history.getTotalDuration(TimeUnit.HOURS), is(24L));
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
        assertThat(repository.getDescriptor(Repository.OPTION_TRANSACTIONS_SUPPORTED), is("true"));
        assertThat(repository.getDescriptor(Repository.OPTION_VERSIONING_SUPPORTED), is("true"));
        assertThat(repository.getDescriptor(Repository.QUERY_XPATH_DOC_ORDER), is("false"));
        assertThat(repository.getDescriptor(Repository.QUERY_XPATH_POS_INDEX), is("false"));
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

        assertThat(repository.runningState().activeSessionCount(), is(0));
    }

    @Test
    @FixFor( "MODE-2190" )
    public void shouldCleanupLocks() throws Exception {
        JcrSession locker1 = repository.login();

        // Create a node to lock
        javax.jcr.Node sessionLockedNode1 = locker1.getRootNode().addNode("sessionLockedNode1");
        sessionLockedNode1.addMixin("mix:lockable");

        javax.jcr.Node openLockedNode = locker1.getRootNode().addNode("openLockedNode");
        openLockedNode.addMixin("mix:lockable");
        locker1.save();

        // Create a session-scoped lock (not deep)
        locker1.getWorkspace().getLockManager().lock(sessionLockedNode1.getPath(), false, true, 1, "me");
        assertLocking(locker1, "/sessionLockedNode1", true);

        // Create an open-scoped lock (not deep)
        locker1.getWorkspace().getLockManager().lock(openLockedNode.getPath(), false, false, 1, "me");
        assertLocking(locker1, "/openLockedNode", true);

        JcrSession locker2 = repository.login();

        javax.jcr.Node sessionLockedNode2 = locker2.getRootNode().addNode("sessionLockedNode2");
        sessionLockedNode2.addMixin("mix:lockable");
        locker2.save();

        locker2.getWorkspace().getLockManager().lock(sessionLockedNode2.getPath(), false, true, 1, "me");
        assertEquals(Long.MAX_VALUE, locker2.getWorkspace().getLockManager().getLock("/sessionLockedNode2").getSecondsRemaining());
        assertEquals(Long.MIN_VALUE, locker1.getWorkspace().getLockManager().getLock("/sessionLockedNode2").getSecondsRemaining());

        assertLocking(locker2, "/openLockedNode", true);
        assertLocking(locker2, "/sessionLockedNode1", true);
        assertLocking(locker2, "/sessionLockedNode2", true);

        javax.jcr.Session reader = repository.login();

        assertLocking(locker1, "/openLockedNode", true);
        assertLocking(locker1, "/sessionLockedNode1", true);
        assertLocking(locker1, "/sessionLockedNode2", true);

        assertLocking(locker2, "/openLockedNode", true);
        assertLocking(locker2, "/sessionLockedNode1", true);
        assertLocking(locker2, "/sessionLockedNode2", true);

        assertLocking(reader, "/openLockedNode", true);
        assertLocking(reader, "/sessionLockedNode1", true);
        assertLocking(reader, "/sessionLockedNode2", true);

        //remove the 1st locking session internally, as if it had terminated unexpectedly
        repository.runningState().removeSession(locker1);
        //make sure the open lock also expires
        Thread.sleep(1001);
        // The locker1 thread should be inactive and both the session lock and the open lock cleaned up
        repository.runningState().cleanUpLocks();

        assertLocking(locker1, "/openLockedNode", false);
        assertLocking(locker1, "/sessionLockedNode1", false);
        assertLocking(locker1, "/sessionLockedNode2", true);

        assertLocking(locker2, "/openLockedNode", false);
        assertLocking(locker2, "/sessionLockedNode1", false);
        assertLocking(locker2, "/sessionLockedNode2", true);

        assertLocking(reader, "/openLockedNode", false);
        assertLocking(reader, "/sessionLockedNode1", false);
        assertLocking(reader, "/sessionLockedNode2", true);

        assertEquals(Long.MAX_VALUE, locker2.getWorkspace().getLockManager().getLock("/sessionLockedNode2").getSecondsRemaining());
    }

    private void assertLocking( Session session, String path, boolean locked ) throws Exception {
        Node node = session.getNode(path);
        if (locked) {
            assertTrue(node.isLocked());
            assertTrue(node.hasProperty(JcrLexicon.LOCK_IS_DEEP.getString()));
            assertTrue(node.hasProperty(JcrLexicon.LOCK_OWNER.getString()));
        } else {
            assertFalse(node.isLocked());
            assertFalse(node.hasProperty(JcrLexicon.LOCK_IS_DEEP.getString()));
            assertFalse(node.hasProperty(JcrLexicon.LOCK_OWNER.getString()));
        }
    }

    @Test
    public void shouldAllowCreatingWorkspaces() throws Exception {
        shutdownDefaultRepository();

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

    @FixFor( {"MODE-1498", "MODE-2202"} )
    @Test
    public void shouldWorkWithUserDefinedTransactionsInSeparateThreads() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        CyclicBarrier completionBarrier = new CyclicBarrier(3);
        session = createSession();

        // THREAD 1 ...
        SessionWorker worker1 = new SessionWorker(session, barrier, completionBarrier, "thread 1") {
            @Override
            protected void execute( Session session ) throws Exception {
                // STEP 1: Setup the listener, and check that the new nodes are not visible to the other session ...
                SimpleListener listener = addListener(3); // we'll create 3 nodes ...
                assertThat(listener.getActualEventCount(), is(0));
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                barrier.await();

                // STEP 2: Make changes using a transaction, but do not commit the transaction ...
                final TransactionManager txnMgr = getTransactionManager();
                txnMgr.begin();
                assertThat(listener.getActualEventCount(), is(0));
                Node txnNode1 = session.getRootNode().addNode("txnNode1");
                Node txnNode1a = txnNode1.addNode("txnNodeA");
                assertThat(txnNode1a, is(notNullValue()));
                assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
                assertThat(session.getRootNode().hasNode("txnNode2"), is(false));
                session.save();
                assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
                assertThat(session.getRootNode().hasNode("txnNode2"), is(false));
                assertThat(listener.getActualEventCount(), is(0));
                barrier.await();

                // STEP 3: Wait for other session to verify that it can't see the new node we created but have not committed...
                // Meanwhile, sleep a bit to let any incorrect events propagate through the system. Our listener still should
                // not see the event, since we didn't commit ...
                Thread.sleep(100L);
                assertThat(listener.getActualEventCount(), is(0));
                barrier.await();

                // STEP 4: Create another new node using this transaction ...
                Node txnNode2 = session.getRootNode().addNode("txnNode2");
                assertThat(txnNode2, is(notNullValue()));
                session.save();
                assertThat(listener.getActualEventCount(), is(0));
                assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
                assertThat(session.getRootNode().hasNode("txnNode2"), is(true));
                barrier.await();

                // STEP 5: Wait for other session to verify that it can't see the new node we created but have not committed...
                // Meanwhile, sleep a bit to let any incorrect events propagate through the system. Our listener still should
                // not see the event, since we didn't commit ...
                Thread.sleep(100L);
                assertThat(listener.getActualEventCount(), is(0));
                assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
                assertThat(session.getRootNode().hasNode("txnNode2"), is(true));
                barrier.await();

                // STEP 6: Commit the txn ...
                txnMgr.commit();
                barrier.await();

                // STEP 7: Verify the commit resulted in the things we expect ...
                listener.waitForEvents();
                nodeExists(session, "/", "txnNode1");
                nodeExists(session, "/", "txnNode2");
            }
        };

        // THREAD 2 ...
        SessionWorker worker2 = new SessionWorker(createSession(), barrier, completionBarrier, "thread 2") {
            @Override
            protected void execute( Session session ) throws Exception {
                // STEP 1: Check that the new nodes are not visible to the other session ...
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                barrier.await();

                // STEP 2: Wait for changes to be made in the other transaction ...
                Thread.sleep(50L);
                barrier.await();

                // STEP 3: Check that we cannot see the new node created by the other session in the still-ongoing txn ...
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                session.refresh(false);
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                barrier.await();

                // STEP 4: Wait for changes to be made in the other transaction ...
                Thread.sleep(50L);
                barrier.await();

                // STEP 5: Check that we cannot see the new node created by the other session in the still-ongoing txn ...
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                session.refresh(false);
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                barrier.await();

                // STEP 6: Wait for the transaction to be committed
                Thread.sleep(50L);
                barrier.await();

                // STEP 7: Check that the nodes are now visible to this session ...
                nodeExists(session, "/", "txnNode1");
                nodeExists(session, "/", "txnNode2");
                session.refresh(false);
                nodeExists(session, "/", "txnNode1");
                nodeExists(session, "/", "txnNode2");
            }
        };

        new Thread(worker1).start();
        new Thread(worker2).start();

        // Wait for the threads to complete ...
        completionBarrier.await();
    }

    @FixFor( {"MODE-1498", "MODE-2202"} )
    @Test
    public void shouldWorkWithUserDefinedTransactionsThatUseRollbackInSeparateThreads() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        CyclicBarrier completionBarrier = new CyclicBarrier(3);
        session = createSession();

        // THREAD 1 ...
        SessionWorker worker1 = new SessionWorker(session, barrier, completionBarrier, "thread 1") {
            @Override
            protected void execute( Session session ) throws Exception {
                // STEP 1: Setup the listener, and check that the new nodes are not visible to the other session ...
                SimpleListener listener = addListener(3); // we'll create 3 nodes ...
                assertThat(listener.getActualEventCount(), is(0));
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                barrier.await();

                // STEP 2: Make changes using a transaction, but do not commit the transaction ...
                final TransactionManager txnMgr = getTransactionManager();
                txnMgr.begin();
                assertThat(listener.getActualEventCount(), is(0));
                Node txnNode1 = session.getRootNode().addNode("txnNode1");
                Node txnNode1a = txnNode1.addNode("txnNodeA");
                assertThat(txnNode1a, is(notNullValue()));
                assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
                assertThat(session.getRootNode().hasNode("txnNode2"), is(false));
                session.save();
                assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
                assertThat(session.getRootNode().hasNode("txnNode2"), is(false));
                assertThat(listener.getActualEventCount(), is(0));
                barrier.await();

                // STEP 3: Wait for other session to verify that it can't see the new node we created but have not committed...
                // Meanwhile, sleep a bit to let any incorrect events propagate through the system. Our listener still should
                // not see the event, since we didn't commit ...
                Thread.sleep(100L);
                assertThat(listener.getActualEventCount(), is(0));
                barrier.await();

                // STEP 4: Create another new node using this transaction ...
                Node txnNode2 = session.getRootNode().addNode("txnNode2");
                assertThat(txnNode2, is(notNullValue()));
                session.save();
                assertThat(listener.getActualEventCount(), is(0));
                assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
                assertThat(session.getRootNode().hasNode("txnNode2"), is(true));
                barrier.await();

                // STEP 5: Wait for other session to verify that it can't see the new node we created but have not committed...
                // Meanwhile, sleep a bit to let any incorrect events propagate through the system. Our listener still should
                // not see the event, since we didn't commit ...
                Thread.sleep(100L);
                assertThat(listener.getActualEventCount(), is(0));
                assertThat(session.getRootNode().hasNode("txnNode1"), is(true));
                assertThat(session.getRootNode().hasNode("txnNode2"), is(true));
                barrier.await();

                // STEP 6: Rollback the txn ...
                txnMgr.rollback();
                barrier.await();

                // STEP 7: Check that there were no events and that the nodes are not visible anymore ...
                Thread.sleep(100L);
                assertThat(listener.getActualEventCount(), is(0));
                assertThat(session.getRootNode().hasNode("txnNode1"), is(false));
                assertThat(session.getRootNode().hasNode("txnNode2"), is(false));
                session.refresh(false);
                assertThat(session.getRootNode().hasNode("txnNode1"), is(false));
                assertThat(session.getRootNode().hasNode("txnNode2"), is(false));
            }
        };

        // THREAD 2 ...
        SessionWorker worker2 = new SessionWorker(createSession(), barrier, completionBarrier, "thread 2") {
            @Override
            protected void execute( Session session ) throws Exception {
                // STEP 1: Check that the new nodes are not visible to the other session ...
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                barrier.await();

                // STEP 2: Wait for changes to be made in the other transaction ...
                Thread.sleep(50L);
                barrier.await();

                // STEP 3: Check that we cannot see the new node created by the other session in the still-ongoing txn ...
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                session.refresh(false);
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                barrier.await();

                // STEP 4: Wait for changes to be made in the other transaction ...
                Thread.sleep(50L);
                barrier.await();

                // STEP 5: Check that we cannot see the new node created by the other session in the still-ongoing txn ...
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                session.refresh(false);
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                barrier.await();

                // STEP 6: Wait for the transaction to be rolled back ...
                Thread.sleep(50L);
                barrier.await();

                // STEP 7: Check that the node IS NOT visible to this session ...
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
                session.refresh(false);
                nodeDoesNotExist(session, "/", "txnNode1");
                nodeDoesNotExist(session, "/", "txnNode2");
            }
        };

        new Thread(worker1).start();
        new Thread(worker2).start();

        // Wait for the threads to complete ...
        completionBarrier.await();
    }

    protected abstract class SessionWorker implements Runnable {

        protected final CyclicBarrier barrier;
        private final CyclicBarrier completionBarrier;
        protected final JcrSession session;
        private final String desc;

        protected SessionWorker( JcrSession session,
                                 CyclicBarrier barrier,
                                 CyclicBarrier completionBarrier,
                                 String desc ) {
            this.barrier = barrier;
            this.session = session;
            this.completionBarrier = completionBarrier;
            this.desc = desc;
        }

        @Override
        public void run() {
            if (session == null) return;
            if (print) System.out.println("Start " + desc);
            try {
                execute(session);
                completionBarrier.await();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                session.logout();
                if (print) System.out.println("Stop " + desc);
            }
        }

        protected abstract void execute( Session session ) throws Exception;
    }

    @FixFor( "MODE-1828" )
    @Test
    public void shouldAllowNodeTypeChangeAfterWrite() throws Exception {
        session = createSession();
        session.workspace()
               .getNodeTypeManager()
               .registerNodeTypes(getClass().getResourceAsStream("/cnd/nodeTypeChange-initial.cnd"), true);

        Node testRoot = session.getRootNode().addNode("/testRoot", "test:nodeTypeA");
        testRoot.setProperty("fieldA", "foo");
        session.save();

        session.workspace()
               .getNodeTypeManager()
               .registerNodeTypes(getClass().getResourceAsStream("/cnd/nodeTypeChange-next.cnd"), true);

        testRoot = session.getNode("/testRoot");
        assertEquals("foo", testRoot.getProperty("fieldA").getString());
        testRoot.setProperty("fieldB", "bar");
        session.save();

        testRoot = session.getNode("/testRoot");
        assertEquals("foo", testRoot.getProperty("fieldA").getString());
        assertEquals("bar", testRoot.getProperty("fieldB").getString());
    }

    @FixFor( "MODE-1525" )
    @Test
    public void shouldDiscoverCorrectChildNodeType() throws Exception {
        session = createSession();

        InputStream cndStream = getClass().getResourceAsStream("/cnd/medical.cnd");
        assertThat(cndStream, is(notNullValue()));
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(cndStream, true);

        // Now create a person ...
        Node root = session.getRootNode();
        Node person = root.addNode("jsmith", "inf:person");
        person.setProperty("inf:firstName", "John");
        person.setProperty("inf:lastName", "Smith");
        session.save();

        Node doctor = root.addNode("drBarnes", "inf:doctor");
        doctor.setProperty("inf:firstName", "Sally");
        doctor.setProperty("inf:lastName", "Barnes");
        doctor.setProperty("inf:doctorProviderNumber", "12345678-AB");
        session.save();

        Node referral = root.addNode("referral", "nt:unstructured");
        referral.addMixin("er:eReferral");
        assertThat(referral.getMixinNodeTypes()[0].getName(), is("er:eReferral"));
        Node group = referral.addNode("er:gp");
        assertThat(group.getPrimaryNodeType().getName(), is("inf:doctor"));
        // Check that group doesn't specify the first name and last name ...
        assertThat(group.hasProperty("inf:firstName"), is(false));
        assertThat(group.hasProperty("inf:lastName"), is(false));
        session.save();
        // Check that group has a default first name and last name ...
        assertThat(group.getProperty("inf:firstName").getString(), is("defaultFirstName"));
        assertThat(group.getProperty("inf:lastName").getString(), is("defaultLastName"));

        Node docGroup = root.addNode("documentGroup", "inf:documentGroup");
        assertThat(docGroup.getPrimaryNodeType().getName(), is("inf:documentGroup"));
        docGroup.addMixin("er:eReferral");
        Node ergp = docGroup.addNode("er:gp");
        assertThat(ergp.getPrimaryNodeType().getName(), is("inf:doctor"));
        // Check that group doesn't specify the first name and last name ...
        assertThat(ergp.hasProperty("inf:firstName"), is(false));
        assertThat(ergp.hasProperty("inf:lastName"), is(false));
        session.save();
        // Check that group has a default first name and last name ...
        assertThat(ergp.getProperty("inf:firstName").getString(), is("defaultFirstName"));
        assertThat(ergp.getProperty("inf:lastName").getString(), is("defaultLastName"));
    }

    @FixFor( "MODE-1525" )
    @Test
    public void shouldDiscoverCorrectChildNodeTypeButFailOnMandatoryPropertiesWithNoDefaultValues() throws Exception {
        session = createSession();

        InputStream cndStream = getClass().getResourceAsStream("/cnd/medical-invalid-mandatories.cnd");
        assertThat(cndStream, is(notNullValue()));
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(cndStream, true);

        // Now create a person ...
        Node root = session.getRootNode();
        Node person = root.addNode("jsmith", "inf:person");
        person.setProperty("inf:firstName", "John");
        person.setProperty("inf:lastName", "Smith");
        session.save();

        Node doctor = root.addNode("drBarnes", "inf:doctor");
        doctor.setProperty("inf:firstName", "Sally");
        doctor.setProperty("inf:lastName", "Barnes");
        doctor.setProperty("inf:doctorProviderNumber", "12345678-AB");
        session.save();

        Node referral = root.addNode("referral", "nt:unstructured");
        referral.addMixin("er:eReferral");
        assertThat(referral.getMixinNodeTypes()[0].getName(), is("er:eReferral"));
        Node group = referral.addNode("er:gp");
        assertThat(group.getPrimaryNodeType().getName(), is("inf:doctor"));
        try {
            session.save();
            fail("Expected a constraint violation exception");
        } catch (ConstraintViolationException e) {
            // expected, since "inf:firstName" is mandatory but doesn't have a default value
        }

        // Set the missing mandatory properties on the node ...
        group.setProperty("inf:firstName", "Sally");
        group.setProperty("inf:lastName", "Barnes");

        // and now Session.save() will work ...
        session.save();
    }

    @Test
    @FixFor( "MODE-1807" )
    public void shouldRegisterCNDFileWithResidualChildDefinition() throws Exception {
        session = createSession();

        InputStream cndStream = getClass().getResourceAsStream("/cnd/orc.cnd");
        assertThat(cndStream, is(notNullValue()));
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(cndStream, true);

        session.getRootNode().addNode("patient", "orc:patient").addNode("patientcase", "orc:patientcase");
        session.save();

        assertNotNull(session.getNode("/patient/patientcase"));
    }

    @Test
    @FixFor( "MODE-1360" )
    public void shouldHandleMultipleConcurrentReadWriteSessions() throws Exception {
        int numThreads = 100;
        final AtomicBoolean passed = new AtomicBoolean(true);
        final AtomicInteger counter = new AtomicInteger(0);
        final Repository repository = this.repository;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        try {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        Session session = repository.login();
                        session.getRootNode().addNode("n" + counter.getAndIncrement()); // unique name
                        session.save();
                        session.logout();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        passed.set(false);
                    }
                }

            };
            for (int i = 0; i < numThreads; i++) {
                executor.execute(runnable);
            }
            executor.shutdown(); // Disable new tasks from being submitted
        } finally {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                fail("timeout");
            }
            if (!passed.get()) {
                fail("one or more threads got an exception");
            }
        }
    }

    @FixFor( "MODE-1805" )
    @Test
    public void shouldCreateRepositoryInstanceWithQueriesDisabled() throws Exception {
        shutdownDefaultRepository();

        RepositoryConfiguration config = RepositoryConfiguration.read("{ 'name' : 'noQueries', 'query' : { 'enabled' : false } }");
        repository = new JcrRepository(config);
        repository.start();
        try {
            Session session = repository.login();
            assertThat(session, is(notNullValue()));

            // Add some content ...
            Node testNode = session.getRootNode().addNode("repos");
            session.save();
            session.logout();

            session = repository.login();
            Node testNode2 = session.getNode("/repos");
            assertTrue(testNode.isSame(testNode2));
            session.logout();

            // Queries return nothing ...
            session = repository.login();
            Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", Query.JCR_SQL2);
            QueryResult results = query.execute();
            assertTrue(results.getNodes().getSize() == 0);
            session.logout();
        } finally {
            repository.shutdown().get(3L, TimeUnit.SECONDS);
            JTATestUtil.clearJBossJTADefaultStoreLocation();
        }
    }

    @FixFor( "MODE-1902" )
    @Test( expected = RepositoryException.class )
    public void shouldFailToStartWhenNoIndexesExistAndRebuildOptionFailIfMissing() throws Exception {
        shutdownDefaultRepository();

        RepositoryConfiguration config = RepositoryConfiguration.read(getClass().getClassLoader()
                                                                                .getResourceAsStream("config/repo-config-fail-if-missing-indexes.json"),
                                                                      "Fail if missing indexes");
        repository = new JcrRepository(config);
        repository.start();
    }

    @Test
    @FixFor( "MODE-2056")
    public void shouldReturnActiveSessions() throws Exception {
        shutdownDefaultRepository();

        config = new RepositoryConfiguration("repoName", environment);
        repository = new JcrRepository(config);
        assertEquals(0, repository.getActiveSessionsCount());

        repository.start();

        JcrSession session1 = repository.login();
        JcrSession session2 = repository.login();
        assertEquals(2, repository.getActiveSessionsCount());
        session2.logout();
        assertEquals(1, repository.getActiveSessionsCount());
        session1.logout();
        assertEquals(0, repository.getActiveSessionsCount());
        repository.login();
        repository.shutdown().get();
        assertEquals(0, repository.getActiveSessionsCount());
    }

    @FixFor( "MODE-2033" )
    @Test
    public void shouldStartAndReturnStartupProblems() throws Exception {
        shutdownDefaultRepository();
        RepositoryConfiguration config = RepositoryConfiguration.read(
                getClass().getClassLoader().getResourceAsStream("config/repo-config-with-startup-problems.json"), "Deprecated config");
        repository = new JcrRepository(config);
        Problems problems = repository.getStartupProblems();
        assertEquals("Expected 2 startup warnings:" + problems.toString(), 2, problems.warningCount());
        assertEquals("Expected 2 startup errors: " + problems.toString(), 2, problems.errorCount());
    }

    @FixFor( "MODE-2033" )
    @Test
    public void shouldClearStartupProblemsOnRestart() throws Exception {
        shutdownDefaultRepository();
        RepositoryConfiguration config = RepositoryConfiguration.read(
                getClass().getClassLoader().getResourceAsStream("config/repo-config-with-startup-problems.json"), "Deprecated config");
        repository = new JcrRepository(config);
        Problems problems = repository.getStartupProblems();
        assertEquals("Invalid startup problems:" + problems.toString(), 4, problems.size());
        repository.shutdown().get();
        problems = repository.getStartupProblems();
        assertEquals("Invalid startup problems:" + problems.toString(), 4, problems.size());
    }

    @FixFor( "MODE-2033" )
    @Test
    public void shouldReturnStartupProblemsAfterStarting() throws Exception {
        shutdownDefaultRepository();
        RepositoryConfiguration config = RepositoryConfiguration.read(
                getClass().getClassLoader().getResourceAsStream("config/repo-config-with-startup-problems.json"),
                "Deprecated config");
        repository = new JcrRepository(config);
        repository.start();
        Problems problems = repository.getStartupProblems();
        assertEquals("Expected 2 startup warnings:" + problems.toString(), 2, problems.warningCount());
        assertEquals("Expected 2 startup errors: " + problems.toString(), 2, problems.errorCount());
    }

    @Test
    @FixFor( "MODE-2140" )
    public void shouldNotAllowNodeTypeRemovalWithQueryPlaceholderConfiguration() throws Exception {
        shutdownDefaultRepository();
        FileUtil.delete("target/indexes");
        RepositoryConfiguration config = RepositoryConfiguration.read(getClass().getClassLoader().getResource(
                "config/repoc-config-query-placeholder.json"));
        repository = new JcrRepository(config);
        String namespaceName = "admb";
        String namespaceUri = "http://www.admb.be/modeshape/admb/1.0";
        String nodeTypeName = "test";

        Session session = repository.login();
        Workspace workspace = session.getWorkspace();
        NamespaceRegistry namespaceRegistry = workspace.getNamespaceRegistry();
        NodeTypeManager nodeTypeManager = workspace.getNodeTypeManager();

        namespaceRegistry.registerNamespace(namespaceName, namespaceUri);

        NodeTypeTemplate nodeTypeTemplate = nodeTypeManager.createNodeTypeTemplate();
        nodeTypeTemplate.setName(namespaceName.concat(":").concat(nodeTypeName));
        nodeTypeTemplate.setMixin(true);
        NodeType nodeType = nodeTypeManager.registerNodeType(nodeTypeTemplate, false);

        // Now create a node with the newly created nodeType
        Node rootNode = session.getRootNode();
        Node newNode = rootNode.addNode("testNode");
        newNode.addMixin(nodeType.getName());
        session.save();
        //sleep to make sure the node is indexed
        Thread.sleep(100);

        try {
            nodeTypeManager.unregisterNodeType(nodeType.getName());
            fail("Should not be able to remove node type");
        } catch (RepositoryException e) {
            //expected
        }
        session.logout();
    }

    @Test
    @FixFor( "MODE-2167" )
    public void shouldEnableOrDisableACLsBasedOnChanges() throws Exception {
        Session session = repository.login();

        try {
            Node testNode = session.getRootNode().addNode("testNode");
            testNode.addNode("node1");
            testNode.addNode("node2");
            session.save();

            assertFalse(repository.repositoryCache().isAccessControlEnabled());

            SimplePrincipal principalA = SimplePrincipal.newInstance("a");
            SimplePrincipal principalB = SimplePrincipal.newInstance("b");
            SimplePrincipal everyone = SimplePrincipal.newInstance("everyone");
            AccessControlManager acm = session.getAccessControlManager();
            Privilege[] allPriviledges = { acm.privilegeFromName(Privilege.JCR_ALL) };


            AccessControlList aclNode1 = getACL(acm, "/testNode/node1");
            aclNode1.addAccessControlEntry(principalA, allPriviledges);
            aclNode1.addAccessControlEntry(principalB, allPriviledges);
            aclNode1.addAccessControlEntry(everyone, allPriviledges);
            acm.setPolicy("/testNode/node1", aclNode1);

            AccessControlList aclNode2 = getACL(acm, "/testNode/node2");
            aclNode2.addAccessControlEntry(principalA, allPriviledges);
            aclNode2.addAccessControlEntry(principalB, allPriviledges);
            aclNode2.addAccessControlEntry(everyone, allPriviledges);
            acm.setPolicy("/testNode/node2", aclNode2);

            //access control should not be enabled yet because we haven't saved the session
            assertFalse(repository.repositoryCache().isAccessControlEnabled());

            session.save();
            assertTrue(repository.repositoryCache().isAccessControlEnabled());

            aclNode1.addAccessControlEntry(everyone, allPriviledges);
            acm.setPolicy("/testNode/node1", aclNode1);
            aclNode2.addAccessControlEntry(everyone, allPriviledges);
            acm.setPolicy("/testNode/node2", aclNode2);

            session.save();
            assertTrue(repository.repositoryCache().isAccessControlEnabled());

            acm.removePolicy("/testNode/node1", null);
            acm.removePolicy("/testNode/node2", null);
            session.save();
            assertFalse(repository.repositoryCache().isAccessControlEnabled());
        } finally {
            session.logout();
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

    private AccessControlList getACL(  AccessControlManager acm, String absPath ) throws Exception {
        AccessControlPolicyIterator it = acm.getApplicablePolicies(absPath);
        if (it.hasNext()) {
            return (AccessControlList)it.nextAccessControlPolicy();
        }
        return (AccessControlList)acm.getPolicies(absPath)[0];
    }

}
