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

package org.modeshape.repository.sequencer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.observe.LocalObservationBus;
import org.modeshape.graph.property.Path;
import org.modeshape.repository.RepositoryLibrary;
import org.modeshape.repository.service.ServiceAdministrator;

/**
 * @author Randall Hauch
 */
public class SequencingServiceTest {

    public static final String REPOSITORY_SOURCE_NAME = "repository";
    public static final String REPOSITORY_WORKSPACE_NAME = "testRepository-Workspace";

    private RepositoryLibrary sources;
    private SequencingService sequencingService;
    private ExecutionContext executionContext;

    @Before
    public void beforeEach() {
        ExecutionContext context = new ExecutionContext();
        InMemoryRepositorySource configSource = new InMemoryRepositorySource();
        configSource.setDefaultWorkspaceName("default");
        Path configPath = context.getValueFactories().getPathFactory().create("/");

        sources = new RepositoryLibrary(configSource, "default", configPath, context, new LocalObservationBus());
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName(REPOSITORY_SOURCE_NAME);
        sources.addSource(source);

        this.executionContext = new ExecutionContext();
        this.sequencingService = new SequencingService();
        this.sequencingService.setExecutionContext(this.executionContext);
        this.sequencingService.setRepositoryLibrary(sources);
    }

    @After
    public void afterEach() throws Exception {
        this.sequencingService.getAdministrator().shutdown();
        this.sequencingService.getAdministrator().awaitTermination(5, TimeUnit.SECONDS);
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
    @Ignore
    public void shouldExecuteSequencersUponChangesToRepositoryThatMatchSequencerPathExpressions() throws Exception {
        // Add configurations for a sequencer ...
        String name = "MockSequencerA";
        String desc = "A mock sequencer that accumulates the number of times it's called";
        String classname = MockSequencerA.class.getName();
        String[] classpath = null;
        String[] pathExpressions = {"/testnodeC/testnodeD/@description => ."};
        SequencerConfig configA = new SequencerConfig(name, desc, Collections.<String, Object>emptyMap(), classname, classpath,
                                                      pathExpressions);
        sequencingService.addSequencer(configA);

        // Start the sequencing sequencingService and try monitoring the workspace ...
        assertThat(sequencingService.getAdministrator().start().isStarted(), is(true));

        // Create a graph for the source ...
        Graph graph = Graph.create(sources.getSource(REPOSITORY_SOURCE_NAME), executionContext);

        // The sequencer should not yet have run ...
        MockSequencerA sequencerA = (MockSequencerA)sequencingService.getSequencerLibrary().getInstances().get(0);
        assertThat(sequencerA, is(notNullValue()));
        assertThat(sequencerA.getCounter(), is(0));
        sequencerA.setExpectedCount(1);
        assertThat(sequencingService.getSequencerLibrary().getInstances(), hasItem((Sequencer)sequencerA));

        // Cause an event, but not one that the sequencer cares about ...
        graph.batch().create("/testnodeC").with("jcr:primaryType", "nt:unstructured").and().execute();

        // Verify that our sequencer was not called ...
        assertThat(sequencerA.getCounter(), is(0));
        assertThat(sequencingService.getSequencerLibrary().getInstances(), hasItem((Sequencer)sequencerA));

        // Cause another event, but again one that the sequencer does not care about ...
        graph.batch().create("/testnodeC/testnodeD").with("jcr:primaryType", "nt:unstructured").and().execute();

        // Verify that our sequencer was not called ...
        assertThat(sequencerA.getCounter(), is(0));
        assertThat(sequencingService.getSequencerLibrary().getInstances(), hasItem((Sequencer)sequencerA));

        // Now set the property that the sequencer DOES care about ...
        graph.batch().set("description").on("/testnodeC/testnodeD").to("This is the value").and().execute();

        // Wait for the event to be processed and the sequencer to be called ...
        sequencerA.awaitExecution(4, TimeUnit.SECONDS); // wait for the sequencer to be called
        assertThat(sequencerA.getCounter(), is(1));
        assertThat(sequencingService.getSequencerLibrary().getInstances(), hasItem((Sequencer)sequencerA));
    }
}
