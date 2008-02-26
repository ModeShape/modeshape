/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.dna.services.sequencers;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import org.jboss.dna.common.jcr.AbstractJcrRepositoryTest;
import org.jboss.dna.services.util.ISessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class SequencingSystemTest extends AbstractJcrRepositoryTest {

    public static final String REPOSITORY_WORKSPACE_NAME = "testRepository-Workspace";

    private SequencingSystem system;
    private ISessionFactory sessionFactory = new ISessionFactory() {

        public Session createSession( String name ) throws RepositoryException {
            assertThat(name, is(REPOSITORY_WORKSPACE_NAME));
            return SequencingSystemTest.this.getRepository().login(getTestCredentials());
        }
    };

    @Before
    public void beforeEach() throws Exception {
        this.system = new SequencingSystem();
        this.system.setSessionFactory(this.sessionFactory);
    }

    @After
    public void afterEach() throws Exception {
        this.system.shutdown();
    }

    @Test
    public void shouldHaveTheDefaultEventFilterUponConstruction() {
        assertThat(system.getEventFilter(), is(sameInstance(SequencingSystem.DEFAULT_EVENT_FILTER)));
    }

    @Test
    public void shouldHaveTheDefaultSelectorUponConstruction() {
        assertThat(system.getSequencerSelector(), is(sameInstance(SequencingSystem.DEFAULT_SEQUENCER_SELECTOR)));
    }

    @Test
    public void shouldHaveSequencingLibraryUponConstruction() {
        assertThat(system.getSequencerLibrary(), is(notNullValue()));
    }

    @Test
    public void shouldHaveNoExecutorServiceUponConstruction() {
        assertThat(system.getExecutorService(), is(nullValue()));
    }

    @Test
    public void shouldCreateDefaultExecutorServiceWhenStartedIfNoExecutorServiceHasBeenSet() {
        assertThat(system.getExecutorService(), is(nullValue()));
        system.start();
        assertThat(system.getExecutorService(), is(notNullValue()));
    }

    @Test
    public void shouldCreateExecutorServiceWhenStarted() {
        assertThat(system.getExecutorService(), is(nullValue()));
        system.start();
        assertThat(system.getExecutorService(), is(notNullValue()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToSetStateToUnknownString() {
        system.setState("asdf");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToSetStateToNullString() {
        system.setState((String)null);
    }

    @Test
    public void shouldSetStateUsingLowercaseString() {
        assertThat(system.setState("started").isStarted(), is(true));
        assertThat(system.setState("paused").isPaused(), is(true));
        assertThat(system.setState("shutdown").isShutdown(), is(true));

        assertThat(system.setState("StarTeD").isStarted(), is(true));
        assertThat(system.setState("PauSed").isPaused(), is(true));
        assertThat(system.setState("ShuTDowN").isShutdown(), is(true));

        assertThat(system.setState("STARTED").isStarted(), is(true));
        assertThat(system.setState("PAUSED").isPaused(), is(true));
        assertThat(system.setState("SHUTDOWN").isShutdown(), is(true));
    }

    @Test
    public void shouldBePausedUponConstruction() {
        assertThat(system.isPaused(), is(true));
        assertThat(system.getState(), is(SequencingSystem.State.PAUSED));
        assertThat(system.isShutdown(), is(false));
        assertThat(system.isStarted(), is(false));
    }

    @Test
    public void shouldBeAbleToShutdownWhenNotStarted() {
        assertThat(system.isShutdown(), is(false));
        for (int i = 0; i != 3; ++i) {
            assertThat(system.shutdown().isShutdown(), is(true));
            assertThat(system.isPaused(), is(false));
            assertThat(system.isStarted(), is(false));
            assertThat(system.getState(), is(SequencingSystem.State.SHUTDOWN));
        }
    }

    @Test
    public void shouldBeAbleToBeRestartedAfterBeingShutdown() {
        assertThat(system.isShutdown(), is(false));
        for (int i = 0; i != 3; ++i) {
            // Shut it down ...
            assertThat(system.shutdown().isShutdown(), is(true));
            assertThat(system.isPaused(), is(false));
            assertThat(system.isStarted(), is(false));
            assertThat(system.getState(), is(SequencingSystem.State.SHUTDOWN));

            // Now start it back up ...
            assertThat(system.start().isStarted(), is(true));
            assertThat(system.isPaused(), is(false));
            assertThat(system.isShutdown(), is(false));
            assertThat(system.getState(), is(SequencingSystem.State.STARTED));

            // Now pause it ...
            assertThat(system.pause().isPaused(), is(true));
            assertThat(system.isStarted(), is(false));
            assertThat(system.isShutdown(), is(false));
            assertThat(system.getState(), is(SequencingSystem.State.PAUSED));

            // Now start it back up ...
            assertThat(system.start().isStarted(), is(true));
            assertThat(system.isPaused(), is(false));
            assertThat(system.isShutdown(), is(false));
            assertThat(system.getState(), is(SequencingSystem.State.STARTED));
        }
    }

    @Test
    public void shouldBeAbleToMonitorWorkspaceWhenPausedOrStarted() throws Exception {
        startRepository();
        Session session = getRepository().login(getTestCredentials());

        // Try when paused ...
        assertThat(system.isPaused(), is(true));
        SequencingSystem.WorkspaceListener listener = system.monitor(REPOSITORY_WORKSPACE_NAME, Event.NODE_ADDED);
        assertThat(listener, is(notNullValue()));
        assertThat(listener.getAbsolutePath(), is("/"));
        assertThat(listener.getEventTypes(), is(Event.NODE_ADDED));

        // Cause an event ...
        session.getRootNode().addNode("testnodeA", "nt:unstructured");
        session.save();
        Thread.sleep(100); // let the events be handled ...
        assertThat(system.getStatistics().getNumberOfEventsIgnored(), is((long)1));

        // Reset the statistics and remove the listener ...
        system.getStatistics().reset();
        assertThat(listener.isRegistered(), is(true));
        listener.unregister();
        assertThat(listener.isRegistered(), is(false));

        // Start the sequencing system and try monitoring the workspace ...
        assertThat(system.start().isStarted(), is(true));
        SequencingSystem.WorkspaceListener listener2 = system.monitor(REPOSITORY_WORKSPACE_NAME, Event.NODE_ADDED);
        assertThat(listener2.isRegistered(), is(true));
        assertThat(listener2, is(notNullValue()));
        assertThat(listener2.getAbsolutePath(), is("/"));
        assertThat(listener2.getEventTypes(), is(Event.NODE_ADDED));

        // Cause an event ...
        session.getRootNode().addNode("testnodeB", "nt:unstructured");
        session.save();
        Thread.sleep(100); // let the events be handled ...

        // Check the results: nothing ignored, and 1 node skipped (since no sequencers apply)
        assertThat(system.getStatistics().getNumberOfEventsIgnored(), is((long)0));
        assertThat(system.getStatistics().getNumberOfNodesSkipped(), is((long)1));

        system.shutdown();
        system.getStatistics().reset();

        // Cause another event ...
        session.getRootNode().addNode("testnodeC", "nt:unstructured");
        session.save();
        Thread.sleep(100); // let the events be handled ...
        // The listener should no longer be registered ...
        assertThat(listener2.isRegistered(), is(false));

        // Check the results: nothing ignored, and nothing skipped
        assertThat(system.getStatistics().getNumberOfEventsIgnored(), is((long)0));
        assertThat(system.getStatistics().getNumberOfNodesSkipped(), is((long)0));
    }

    @Test
    public void shouldUnregisterAllWorkspaceListenersWhenSystemIsShutdownAndNotWhenPaused() throws Exception {
        startRepository();
        Session session = getRepository().login(getTestCredentials());

        // Start the sequencing system and try monitoring the workspace ...
        assertThat(system.start().isStarted(), is(true));
        SequencingSystem.WorkspaceListener listener = system.monitor(REPOSITORY_WORKSPACE_NAME, Event.NODE_ADDED);
        assertThat(listener.isRegistered(), is(true));
        assertThat(listener, is(notNullValue()));
        assertThat(listener.getAbsolutePath(), is("/"));
        assertThat(listener.getEventTypes(), is(Event.NODE_ADDED));

        // Cause an event ...
        session.getRootNode().addNode("testnodeB", "nt:unstructured");
        session.save();
        assertThat(listener.isRegistered(), is(true));

        // Pause the system, can cause an event ...
        system.pause();
        session.getRootNode().addNode("testnodeB", "nt:unstructured");
        session.save();
        assertThat(listener.isRegistered(), is(true));

        system.shutdown();

        // Cause another event ...
        session.getRootNode().addNode("testnodeC", "nt:unstructured");
        session.save();

        // The listener should no longer be registered ...
        assertThat(listener.isRegistered(), is(false));
    }
}
