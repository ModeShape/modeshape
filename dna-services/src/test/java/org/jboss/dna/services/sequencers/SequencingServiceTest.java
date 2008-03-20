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
import org.jboss.dna.services.ServiceAdministrator;
import org.jboss.dna.services.SessionFactory;
import org.jboss.dna.services.observation.ObservationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class SequencingServiceTest extends AbstractJcrRepositoryTest {

    public static final String REPOSITORY_WORKSPACE_NAME = "testRepository-Workspace";

    private ObservationService observationService;
    private SequencingService system;
    private SessionFactory sessionFactory;

    @Before
    public void beforeEach() throws Exception {
        this.sessionFactory = new SessionFactory() {

            public Session createSession( String name ) throws RepositoryException {
                assertThat(name, is(REPOSITORY_WORKSPACE_NAME));
                return SequencingServiceTest.this.getRepository().login(getTestCredentials());
            }
        };
        this.system = new SequencingService();
        this.system.setSessionFactory(this.sessionFactory);
        this.observationService = new ObservationService(this.sessionFactory);
        this.observationService.addListener(this.system);
    }

    @After
    public void afterEach() throws Exception {
        this.system.getAdministrator().shutdown();
    }

    @Test
    public void shouldHaveTheDefaultSelectorUponConstruction() {
        assertThat(system.getSequencerSelector(), is(sameInstance(SequencingService.DEFAULT_SEQUENCER_SELECTOR)));
    }

    @Test
    public void shouldHaveNoExecutorServiceUponConstruction() {
        assertThat(system.getExecutorService(), is(nullValue()));
    }

    @Test
    public void shouldCreateDefaultExecutorServiceWhenStartedIfNoExecutorServiceHasBeenSet() {
        assertThat(system.getExecutorService(), is(nullValue()));
        system.getAdministrator().start();
        assertThat(system.getExecutorService(), is(notNullValue()));
    }

    @Test
    public void shouldCreateExecutorServiceWhenStarted() {
        assertThat(system.getExecutorService(), is(nullValue()));
        system.getAdministrator().start();
        assertThat(system.getExecutorService(), is(notNullValue()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToSetStateToUnknownString() {
        system.getAdministrator().setState("asdf");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToSetStateToNullString() {
        system.getAdministrator().setState((String)null);
    }

    @Test
    public void shouldSetStateUsingLowercaseString() {
        assertThat(system.getAdministrator().setState("started").isStarted(), is(true));
        assertThat(system.getAdministrator().setState("paused").isPaused(), is(true));
        assertThat(system.getAdministrator().setState("shutdown").isShutdown(), is(true));
    }

    @Test
    public void shouldSetStateUsingMixedCaseString() {
        assertThat(system.getAdministrator().setState("StarTeD").isStarted(), is(true));
        assertThat(system.getAdministrator().setState("PauSed").isPaused(), is(true));
        assertThat(system.getAdministrator().setState("ShuTDowN").isShutdown(), is(true));
    }

    @Test
    public void shouldSetStateUsingUppercasString() {
        assertThat(system.getAdministrator().setState("STARTED").isStarted(), is(true));
        assertThat(system.getAdministrator().setState("PAUSED").isPaused(), is(true));
        assertThat(system.getAdministrator().setState("SHUTDOWN").isShutdown(), is(true));
    }

    @Test
    public void shouldBePausedUponConstruction() {
        assertThat(system.getAdministrator().isPaused(), is(true));
        assertThat(system.getAdministrator().getState(), is(ServiceAdministrator.State.PAUSED));
        assertThat(system.getAdministrator().isShutdown(), is(false));
        assertThat(system.getAdministrator().isStarted(), is(false));
    }

    @Test
    public void shouldBeAbleToShutdownWhenNotStarted() {
        assertThat(system.getAdministrator().isShutdown(), is(false));
        for (int i = 0; i != 3; ++i) {
            assertThat(system.getAdministrator().shutdown().isShutdown(), is(true));
            assertThat(system.getAdministrator().isPaused(), is(false));
            assertThat(system.getAdministrator().isStarted(), is(false));
            assertThat(system.getAdministrator().getState(), is(ServiceAdministrator.State.SHUTDOWN));
        }
    }

    @Test
    public void shouldBeAbleToBePauseAndRestarted() {
        assertThat(system.getAdministrator().isShutdown(), is(false));
        for (int i = 0; i != 3; ++i) {
            // Now pause it ...
            assertThat(system.getAdministrator().pause().isPaused(), is(true));
            assertThat(system.getAdministrator().isStarted(), is(false));
            assertThat(system.getAdministrator().isShutdown(), is(false));
            assertThat(system.getAdministrator().getState(), is(ServiceAdministrator.State.PAUSED));

            // Now start it back up ...
            assertThat(system.getAdministrator().start().isStarted(), is(true));
            assertThat(system.getAdministrator().isPaused(), is(false));
            assertThat(system.getAdministrator().isShutdown(), is(false));
            assertThat(system.getAdministrator().getState(), is(ServiceAdministrator.State.STARTED));
        }
    }

    @Test( expected = IllegalStateException.class )
    public void shouldNotBeAbleToBeRestartedAfterBeingShutdown() {
        assertThat(system.getAdministrator().isShutdown(), is(false));
        // Shut it down ...
        assertThat(system.getAdministrator().shutdown().isShutdown(), is(true));
        assertThat(system.getAdministrator().isPaused(), is(false));
        assertThat(system.getAdministrator().isStarted(), is(false));
        assertThat(system.getAdministrator().getState(), is(ServiceAdministrator.State.SHUTDOWN));

        // Now start it back up ... this will fail
        system.getAdministrator().start();
    }

    @Test
    public void shouldBeAbleToMonitorWorkspaceWhenPausedOrStarted() throws Exception {
        startRepository();
        Session session = getRepository().login(getTestCredentials());

        // Try when paused ...
        assertThat(system.getAdministrator().isPaused(), is(true));
        assertThat(observationService.getAdministrator().pause().isPaused(), is(true));
        ObservationService.WorkspaceListener listener = observationService.monitor(REPOSITORY_WORKSPACE_NAME, Event.NODE_ADDED);
        assertThat(listener, is(notNullValue()));
        assertThat(listener.getAbsolutePath(), is("/"));
        assertThat(listener.getEventTypes(), is(Event.NODE_ADDED));

        // Cause an event ...
        session.getRootNode().addNode("testnodeA", "nt:unstructured");
        session.save();
        Thread.sleep(100); // let the events be handled ...
        assertThat(observationService.getStatistics().getNumberOfEventsIgnored(), is((long)1));

        // Reset the statistics and remove the listener ...
        system.getStatistics().reset();
        observationService.getStatistics().reset();
        assertThat(listener.isRegistered(), is(true));
        listener.unregister();
        assertThat(listener.isRegistered(), is(false));

        // Start the sequencing system and try monitoring the workspace ...
        assertThat(system.getAdministrator().start().isStarted(), is(true));
        assertThat(observationService.getAdministrator().start().isStarted(), is(true));
        ObservationService.WorkspaceListener listener2 = observationService.monitor(REPOSITORY_WORKSPACE_NAME, Event.NODE_ADDED);
        assertThat(listener2.isRegistered(), is(true));
        assertThat(listener2, is(notNullValue()));
        assertThat(listener2.getAbsolutePath(), is("/"));
        assertThat(listener2.getEventTypes(), is(Event.NODE_ADDED));

        // Cause an event ...
        session.getRootNode().addNode("testnodeB", "nt:unstructured");
        session.save();
        Thread.sleep(100); // let the events be handled ...

        // Check the results: nothing ignored, and 1 node skipped (since no sequencers apply)
        assertThat(observationService.getStatistics().getNumberOfEventsIgnored(), is((long)0));
        assertThat(system.getStatistics().getNumberOfNodesSkipped(), is((long)1));

        system.getAdministrator().shutdown();
        system.getStatistics().reset();
        observationService.getAdministrator().shutdown();
        observationService.getStatistics().reset();

        // Cause another event ...
        session.getRootNode().addNode("testnodeC", "nt:unstructured");
        session.save();
        Thread.sleep(100); // let the events be handled ...
        // The listener should no longer be registered ...
        assertThat(listener2.isRegistered(), is(false));

        // Check the results: nothing ignored, and nothing skipped
        assertThat(observationService.getStatistics().getNumberOfEventsIgnored(), is((long)0));
        assertThat(system.getStatistics().getNumberOfNodesSkipped(), is((long)0));
    }

    @Test
    public void shouldUnregisterAllWorkspaceListenersWhenSystemIsShutdownAndNotWhenPaused() throws Exception {
        startRepository();
        Session session = getRepository().login(getTestCredentials());

        // Start the sequencing system and try monitoring the workspace ...
        assertThat(system.getAdministrator().start().isStarted(), is(true));
        ObservationService.WorkspaceListener listener = observationService.monitor(REPOSITORY_WORKSPACE_NAME, Event.NODE_ADDED);
        assertThat(listener.isRegistered(), is(true));
        assertThat(listener, is(notNullValue()));
        assertThat(listener.getAbsolutePath(), is("/"));
        assertThat(listener.getEventTypes(), is(Event.NODE_ADDED));

        // Cause an event ...
        session.getRootNode().addNode("testnodeB", "nt:unstructured");
        session.save();
        assertThat(listener.isRegistered(), is(true));

        // Pause the system, can cause an event ...
        system.getAdministrator().pause();
        session.getRootNode().addNode("testnodeB", "nt:unstructured");
        session.save();
        assertThat(listener.isRegistered(), is(true));

        system.getAdministrator().shutdown();
        observationService.getAdministrator().shutdown();

        // Cause another event ...
        session.getRootNode().addNode("testnodeC", "nt:unstructured");
        session.save();

        // The listener should no longer be registered ...
        assertThat(listener.isRegistered(), is(false));
    }
}
