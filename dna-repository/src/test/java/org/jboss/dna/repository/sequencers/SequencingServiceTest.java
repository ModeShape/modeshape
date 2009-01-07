/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.dna.repository.sequencers;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import org.jboss.dna.common.jcr.AbstractJcrRepositoryTest;
import org.jboss.dna.repository.observation.ObservationService;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.repository.util.JcrExecutionContext;
import org.jboss.dna.repository.util.MockJcrExecutionContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class SequencingServiceTest extends AbstractJcrRepositoryTest {

    public static final int ALL_EVENT_TYPES = Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED
                                              | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
    public static final String REPOSITORY_WORKSPACE_NAME = "testRepository-Workspace";

    private ObservationService observationService;
    private SequencingService sequencingService;
    private JcrExecutionContext executionContext;

    @Before
    public void beforeEach() {
        this.executionContext = new MockJcrExecutionContext(this, REPOSITORY_WORKSPACE_NAME);
        this.sequencingService = new SequencingService();
        this.sequencingService.setExecutionContext(this.executionContext);
        this.observationService = new ObservationService(this.executionContext.getSessionFactory());
        this.observationService.addListener(this.sequencingService);
    }

    @After
    public void afterEach() throws Exception {
        this.observationService.getAdministrator().shutdown();
        this.observationService.getAdministrator().awaitTermination(5, TimeUnit.SECONDS);
        this.sequencingService.getAdministrator().shutdown();
        this.sequencingService.getAdministrator().awaitTermination(5, TimeUnit.SECONDS);
        super.shutdownRepository();
    }

    @Test
    public void shouldHaveTheDefaultSelectorUponConstruction() {
        assertThat(sequencingService.getSequencerSelector(), is(sameInstance(SequencingService.DEFAULT_SEQUENCER_SELECTOR)));
    }

    @Test
    public void shouldHaveNoExecutorServiceUponConstruction() {
        assertThat(sequencingService.getExecutorService(), is(nullValue()));
    }

    @Test
    public void shouldCreateDefaultExecutorServiceWhenStartedIfNoExecutorServiceHasBeenSet() {
        assertThat(sequencingService.getExecutorService(), is(nullValue()));
        sequencingService.getAdministrator().start();
        assertThat(sequencingService.getExecutorService(), is(notNullValue()));
    }

    @Test
    public void shouldCreateExecutorServiceWhenStarted() {
        assertThat(sequencingService.getExecutorService(), is(nullValue()));
        sequencingService.getAdministrator().start();
        assertThat(sequencingService.getExecutorService(), is(notNullValue()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToSetStateToUnknownString() {
        sequencingService.getAdministrator().setState("asdf");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToSetStateToNullString() {
        sequencingService.getAdministrator().setState((String)null);
    }

    @Test
    public void shouldSetStateUsingLowercaseString() {
        assertThat(sequencingService.getAdministrator().setState("started").isStarted(), is(true));
        assertThat(sequencingService.getAdministrator().setState("paused").isPaused(), is(true));
        assertThat(sequencingService.getAdministrator().setState("shutdown").isShutdown(), is(true));
    }

    @Test
    public void shouldSetStateUsingMixedCaseString() {
        assertThat(sequencingService.getAdministrator().setState("StarTeD").isStarted(), is(true));
        assertThat(sequencingService.getAdministrator().setState("PauSed").isPaused(), is(true));
        assertThat(sequencingService.getAdministrator().setState("ShuTDowN").isShutdown(), is(true));
    }

    @Test
    public void shouldSetStateUsingUppercasString() {
        assertThat(sequencingService.getAdministrator().setState("STARTED").isStarted(), is(true));
        assertThat(sequencingService.getAdministrator().setState("PAUSED").isPaused(), is(true));
        assertThat(sequencingService.getAdministrator().setState("SHUTDOWN").isShutdown(), is(true));
    }

    @Test
    public void shouldBePausedUponConstruction() {
        assertThat(sequencingService.getAdministrator().isPaused(), is(true));
        assertThat(sequencingService.getAdministrator().getState(), is(ServiceAdministrator.State.PAUSED));
        assertThat(sequencingService.getAdministrator().isShutdown(), is(false));
        assertThat(sequencingService.getAdministrator().isStarted(), is(false));
    }

    @Test
    public void shouldBeAbleToShutdownWhenNotStarted() {
        assertThat(sequencingService.getAdministrator().isShutdown(), is(false));
        for (int i = 0; i != 3; ++i) {
            assertThat(sequencingService.getAdministrator().shutdown().isShutdown(), is(true));
            assertThat(sequencingService.getAdministrator().isPaused(), is(false));
            assertThat(sequencingService.getAdministrator().isStarted(), is(false));
            ServiceAdministrator.State actualState = sequencingService.getAdministrator().getState();
            assertThat(actualState == ServiceAdministrator.State.SHUTDOWN || actualState == ServiceAdministrator.State.TERMINATED,
                       is(true));
        }
    }

    @Test
    public void shouldBeAbleToBePauseAndRestarted() {
        assertThat(sequencingService.getAdministrator().isShutdown(), is(false));
        for (int i = 0; i != 3; ++i) {
            // Now pause it ...
            assertThat(sequencingService.getAdministrator().pause().isPaused(), is(true));
            assertThat(sequencingService.getAdministrator().isStarted(), is(false));
            assertThat(sequencingService.getAdministrator().isShutdown(), is(false));
            assertThat(sequencingService.getAdministrator().getState(), is(ServiceAdministrator.State.PAUSED));

            // Now start it back up ...
            assertThat(sequencingService.getAdministrator().start().isStarted(), is(true));
            assertThat(sequencingService.getAdministrator().isPaused(), is(false));
            assertThat(sequencingService.getAdministrator().isShutdown(), is(false));
            assertThat(sequencingService.getAdministrator().getState(), is(ServiceAdministrator.State.STARTED));
        }
    }

    @Test( expected = IllegalStateException.class )
    public void shouldNotBeAbleToBeRestartedAfterBeingShutdown() {
        assertThat(sequencingService.getAdministrator().isShutdown(), is(false));
        // Shut it down ...
        assertThat(sequencingService.getAdministrator().shutdown().isShutdown(), is(true));
        assertThat(sequencingService.getAdministrator().isPaused(), is(false));
        assertThat(sequencingService.getAdministrator().isStarted(), is(false));
        ServiceAdministrator.State actualState = sequencingService.getAdministrator().getState();
        assertThat(actualState == ServiceAdministrator.State.SHUTDOWN || actualState == ServiceAdministrator.State.TERMINATED,
                   is(true));

        // Now start it back up ... this will fail
        sequencingService.getAdministrator().start();
    }

    @Test
    public void shouldBeAbleToMonitorWorkspaceWhenPausedOrStarted() throws Exception {
        startRepository();
        Session session = getRepository().login(getTestCredentials());

        // Try when paused ...
        assertThat(sequencingService.getAdministrator().isPaused(), is(true));
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
        sequencingService.getStatistics().reset();
        observationService.getStatistics().reset();
        assertThat(listener.isRegistered(), is(true));
        listener.unregister();
        assertThat(listener.isRegistered(), is(false));

        // Start the sequencing sequencingService and try monitoring the workspace ...
        assertThat(sequencingService.getAdministrator().start().isStarted(), is(true));
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
        assertThat(sequencingService.getStatistics().getNumberOfNodesSkipped(), is((long)1));

        sequencingService.getAdministrator().shutdown();
        sequencingService.getStatistics().reset();
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
        assertThat(sequencingService.getStatistics().getNumberOfNodesSkipped(), is((long)0));
    }

    @Test
    public void shouldUnregisterAllWorkspaceListenersWhenSystemIsShutdownAndNotWhenPaused() throws Exception {
        startRepository();
        Session session = getRepository().login(getTestCredentials());

        // Start the sequencing sequencingService and try monitoring the workspace ...
        assertThat(sequencingService.getAdministrator().start().isStarted(), is(true));
        ObservationService.WorkspaceListener listener = observationService.monitor(REPOSITORY_WORKSPACE_NAME, Event.NODE_ADDED);
        assertThat(listener.isRegistered(), is(true));
        assertThat(listener, is(notNullValue()));
        assertThat(listener.getAbsolutePath(), is("/"));
        assertThat(listener.getEventTypes(), is(Event.NODE_ADDED));

        // Cause an event ...
        session.getRootNode().addNode("testnodeB", "nt:unstructured");
        session.save();
        assertThat(listener.isRegistered(), is(true));

        // Pause the sequencingService, can cause an event ...
        sequencingService.getAdministrator().pause();
        session.getRootNode().addNode("testnodeB", "nt:unstructured");
        session.save();
        assertThat(listener.isRegistered(), is(true));

        // Shut down the services and await termination ...
        sequencingService.getAdministrator().shutdown();
        observationService.getAdministrator().shutdown();
        sequencingService.getAdministrator().awaitTermination(2, TimeUnit.SECONDS);
        observationService.getAdministrator().awaitTermination(2, TimeUnit.SECONDS);

        // Cause another event ...
        session.getRootNode().addNode("testnodeC", "nt:unstructured");
        session.save();

        // The listener should no longer be registered ...
        assertThat(listener.isRegistered(), is(false));
    }

    @Test
    public void shouldExecuteSequencersUponChangesToRepositoryThatMatchSequencerPathExpressions() throws Exception {
        // Add configurations for a sequencer ...
        String name = "MockSequencerA";
        String desc = "A mock sequencer that accumulates the number of times it's called";
        String classname = MockSequencerA.class.getName();
        String[] classpath = null;
        String[] pathExpressions = {"/testnodeC/testnodeD/@description => ."};
        SequencerConfig configA = new SequencerConfig(name, desc, classname, classpath, pathExpressions);
        sequencingService.addSequencer(configA);

        // Start the repository and get a session ...
        startRepository();
        Session session = getRepository().login(getTestCredentials());

        // Start the sequencing sequencingService and try monitoring the workspace ...
        assertThat(sequencingService.getAdministrator().start().isStarted(), is(true));
        ObservationService.WorkspaceListener listener = observationService.monitor(REPOSITORY_WORKSPACE_NAME, ALL_EVENT_TYPES);
        assertThat(listener.isRegistered(), is(true));
        assertThat(listener, is(notNullValue()));
        assertThat(listener.getAbsolutePath(), is("/"));
        assertThat(listener.getEventTypes(), is(ALL_EVENT_TYPES));

        // The sequencer should not yet have run ...
        MockSequencerA sequencerA = (MockSequencerA)sequencingService.getSequencerLibrary().getInstances().get(0);
        assertThat(sequencerA, is(notNullValue()));
        assertThat(sequencerA.getCounter(), is(0));
        sequencerA.setExpectedCount(1);
        assertThat(sequencingService.getSequencerLibrary().getInstances(), hasItem((Sequencer)sequencerA));

        // Cause an event, but not one that the sequencer cares about ...
        Node nodeC = session.getRootNode().addNode("testnodeC", "nt:unstructured");
        assertThat(nodeC, is(notNullValue()));
        session.save();
        assertThat(sequencerA.getCounter(), is(0));
        assertThat(sequencingService.getSequencerLibrary().getInstances(), hasItem((Sequencer)sequencerA));

        // Cause another event, but again one that the sequencer does not care about ...
        Node nodeD = nodeC.addNode("testnodeD", "nt:unstructured");
        assertThat(nodeD, is(notNullValue()));
        session.save();
        assertThat(sequencerA.getCounter(), is(0));
        assertThat(sequencingService.getSequencerLibrary().getInstances(), hasItem((Sequencer)sequencerA));

        // Now set the property that the sequencer DOES care about ...
        nodeD.setProperty("description", "This is the value");
        session.save();

        // Wait for the event to be processed and the sequencer to be called ...
        sequencerA.awaitExecution(4, TimeUnit.SECONDS); // wait for the sequencer to be called
        assertThat(sequencerA.getCounter(), is(1));
        assertThat(sequencingService.getSequencerLibrary().getInstances(), hasItem((Sequencer)sequencerA));
    }
}
