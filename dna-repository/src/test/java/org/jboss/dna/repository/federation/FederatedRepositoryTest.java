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
package org.jboss.dna.repository.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositorySourceListener;
import org.jboss.dna.spi.graph.connection.TimeDelayingRepositorySource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederatedRepositoryTest {

    private FederatedRepository repository;
    private String name;
    @Mock
    private FederationService service;
    @Mock
    private RepositorySourceListener listener1;
    @Mock
    private RepositorySourceListener listener2;
    @Mock
    private FederatedSource source1;
    @Mock
    private FederatedSource source2;

    // private RepositoryConnectionPool connectionPool;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        name = "Test repository";
        repository = new FederatedRepository(service, name);
        stub(source1.getName()).toReturn("soure 1");
        stub(source2.getName()).toReturn("soure 2");
    }

    @Test
    public void shouldHaveNamePassedIntoConstructor() {
        assertThat(repository.getName(), is(name));
    }

    @Test
    public void shouldHaveFederationServicePassedIntoConstructor() {
        assertThat(repository.getService(), is(sameInstance(service)));
    }

    @Test
    public void shouldHaveAdministrator() {
        assertThat(repository.getAdministrator(), is(notNullValue()));
    }

    @Test
    public void shouldHaveNoListenersUponCreation() {
        assertThat(repository.getListeners(), is(notNullValue()));
        assertThat(repository.getListeners().isEmpty(), is(true));
    }

    @Test
    public void shouldNotAddNullListener() {
        assertThat(repository.addListener(null), is(false));
    }

    @Test
    public void shouldNotAddListenerIfAlreadyInList() {
        assertThat(repository.getListeners().size(), is(0));
        assertThat(repository.addListener(listener1), is(true));
        assertThat(repository.getListeners().size(), is(1));
        assertThat(repository.addListener(listener1), is(false));
        assertThat(repository.getListeners().size(), is(1));
    }

    @Test
    public void shouldAddDifferentListeners() {
        assertThat(repository.getListeners().size(), is(0));
        assertThat(repository.addListener(listener1), is(true));
        assertThat(repository.getListeners().size(), is(1));
        assertThat(repository.addListener(listener2), is(true));
        assertThat(repository.getListeners().size(), is(2));
        assertThat(repository.getListeners(), hasItems(listener1, listener2));
        assertThat(repository.getListeners().get(0), is(sameInstance(listener1)));
        assertThat(repository.getListeners().get(1), is(sameInstance(listener2)));
    }

    @Test
    public void shouldAllowReorderingOfListeners() {
        assertThat(repository.getListeners().size(), is(0));
        assertThat(repository.addListener(listener1), is(true));
        assertThat(repository.addListener(listener2), is(true));
        assertThat(repository.getListeners().size(), is(2));
        assertThat(repository.getListeners(), hasItems(listener1, listener2));
        repository.getListeners().remove(0);
        repository.getListeners().add(1, listener1);
        assertThat(repository.getListeners(), hasItems(listener2, listener1));
        assertThat(repository.getListeners().get(0), is(sameInstance(listener2)));
        assertThat(repository.getListeners().get(1), is(sameInstance(listener1)));
    }

    @Test
    public void shouldAllowRemovalOfListeners() {
        assertThat(repository.getListeners().size(), is(0));
        assertThat(repository.addListener(listener1), is(true));
        assertThat(repository.addListener(listener2), is(true));
        assertThat(repository.getListeners(), hasItems(listener1, listener2));
        assertThat(repository.removeListener(listener1), is(true));
        assertThat(repository.getListeners(), hasItems(listener2));
        assertThat(repository.removeListener(listener2), is(true));
        assertThat(repository.getListeners(), hasItems(new RepositorySourceListener[] {}));
    }

    @Test
    public void shouldNotRemoveListenerThatIsNotAlreadyRegistered() {
        assertThat(repository.getListeners().size(), is(0));
        assertThat(repository.addListener(listener1), is(true));
        assertThat(repository.getListeners().size(), is(1));
        assertThat(repository.removeListener(listener2), is(false));
    }

    @Test
    public void shouldHaveNoSourcesAfterInitialization() {
        assertThat(repository.getSources(), is(notNullValue()));
        assertThat(repository.getSources().isEmpty(), is(true));
    }

    @Test
    public void shouldAddSourceThatIsNotAlreadyRegistered() {
        assertThat(repository.getSources(), hasItems(new FederatedSource[] {}));
        assertThat(repository.addSource(source1), is(true));
        assertThat(repository.getSources(), hasItems(source1));
        assertThat(repository.addSource(source2), is(true));
        assertThat(repository.getSources(), hasItems(source1, source2));
    }

    @Test
    public void shouldNotAddSourceThatIsAlreadyRegistered() {
        String source1Name = source1.getName();
        FederatedSource source1a = mock(FederatedSource.class);
        stub(source1a.getName()).toReturn(source1Name);

        assertThat(repository.getSources(), hasItems(new FederatedSource[] {}));
        assertThat(repository.addSource(source1), is(true));
        assertThat(repository.getSources(), hasItems(source1));
        assertThat(repository.addSource(source2), is(true));
        assertThat(repository.getSources(), hasItems(source1, source2));
        assertThat(repository.addSource(source1a), is(false));
        assertThat(repository.getSources(), hasItems(source1, source2));
    }

    @Test
    public void shouldShutdownAndRemoveRepositoryFromFederationService() {
        repository.getAdministrator().shutdown();
        verify(service, times(1)).removeRepository(repository);
    }

    @Test
    public void shouldShutdownAllSourceConnectionPoolsWhenShuttingDownRepository() throws Exception {
        // Create the source instances that wait during termination ...
        TimeDelayingRepositorySource timeDelaySource1 = new TimeDelayingRepositorySource("time delay source 1");
        source1 = new FederatedSource(timeDelaySource1);
        TimeDelayingRepositorySource timeDelaySource2 = new TimeDelayingRepositorySource("time delay source 2");
        source2 = new FederatedSource(timeDelaySource2);
        repository.addSource(source1);
        repository.addSource(source2);
        assertThat(repository.getSources(), hasItems(source1, source2));

        // Get a connection from one source ...
        RepositoryConnection connection = source2.getConnection();
        assertThat(connection, is(notNullValue()));

        // Shut down the repository, which will shut down each of the sources ...
        repository.getAdministrator().shutdown();
        assertThat(repository.getAdministrator().isShutdown(), is(true));
        assertThat(repository.getAdministrator().isTerminated(), is(false));

        // Source 1 should be shut down AND terminated ...
        assertThat(source1.getConnectionPool().isShutdown(), is(true));
        assertThat(source1.getConnectionPool().isTerminated(), is(true));

        // Source 2 should be shutdown but not terminated, since we still have a connection ...
        assertThat(source2.getConnectionPool().isShutdown(), is(true));
        assertThat(source2.getConnectionPool().isTerminated(), is(false));

        // Close the connection ...
        connection.close();
        assertThat(repository.getAdministrator().awaitTermination(1, TimeUnit.SECONDS), is(true));
        assertThat(repository.getAdministrator().isShutdown(), is(true));
        assertThat(repository.getAdministrator().isTerminated(), is(true));
    }
}
