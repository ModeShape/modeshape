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
import static org.junit.Assert.assertThat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.Repository;
import javax.jcr.ValueFormatException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.jcr.RepositoryStatistics.DurationActivity;
import org.modeshape.jcr.RepositoryStatistics.DurationMetric;
import org.modeshape.jcr.RepositoryStatistics.History;
import org.modeshape.jcr.RepositoryStatistics.Statistics;
import org.modeshape.jcr.RepositoryStatistics.ValueMetric;
import org.modeshape.jcr.RepositoryStatistics.Window;
import org.modeshape.jcr.cache.WorkspaceNotFoundException;

public class JcrRepositoryTest {

    private RepositoryConfiguration config;
    private JcrRepository repository;

    @Before
    public void beforeEach() throws Exception {
        config = new RepositoryConfiguration("repoName");
        repository = new JcrRepository(config);
        repository.start();
    }

    @After
    public void afterEach() throws Exception {
        try {
            repository.shutdown().get(3L, TimeUnit.SECONDS);
        } finally {
            repository = null;
            config = null;
        }
    }

    @Test
    public void shouldAllowCreationOfSessionForDefaultWorkspaceWithoutUsingCredentials() throws Exception {
        JcrSession session1 = repository.login();
        assertThat(session1.isLive(), is(true));
    }

    @Test( expected = WorkspaceNotFoundException.class )
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
        } catch (WorkspaceNotFoundException e) {
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
        } catch (WorkspaceNotFoundException e) {
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
        assertThat(repository.getDescriptor(Repository.REP_NAME_DESC), is("ModeShape JCR Repository"));
        assertThat(repository.getDescriptor(Repository.REP_VENDOR_DESC), is("JBoss, a division of Red Hat"));
        assertThat(repository.getDescriptor(Repository.REP_VENDOR_URL_DESC), is("http://www.modeshape.org"));
        assertThat(repository.getDescriptor(Repository.REP_VERSION_DESC), is(notNullValue()));
        // assertThat(repository.getDescriptor(Repository.REP_VERSION_DESC), is("1.1-SNAPSHOT"));
        assertThat(repository.getDescriptor(Repository.SPEC_NAME_DESC), is(JcrI18n.SPEC_NAME_DESC.text()));
        assertThat(repository.getDescriptor(Repository.SPEC_VERSION_DESC), is("2.0"));
    }

}
