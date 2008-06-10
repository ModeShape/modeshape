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
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;
import org.jboss.dna.spi.graph.connection.RepositorySourceException;
import org.jboss.dna.spi.graph.connection.RepositorySourceListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederatedRepositoryConnectionTest {

    private FederatedRepositoryConnection connection;
    private String sourceName;
    @Mock
    private FederatedRepositorySource source;
    @Mock
    private FederatedRepository repository;
    @Mock
    private CachePolicy defaultCachePolicy;
    @Mock
    private ServiceAdministrator repositoryAdmin;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        sourceName = "Source X";
        stub(source.getName()).toReturn(sourceName);
        stub(repository.getDefaultCachePolicy()).toReturn(defaultCachePolicy);
        stub(repository.getAdministrator()).toReturn(repositoryAdmin);
        connection = new FederatedRepositoryConnection(repository, source);
    }

    @Test
    public void shouldHaveSourceAndRepository() {
        assertThat(connection.getRepository(), is(repository));
        assertThat(connection.getRepositorySource(), is(source));
    }

    @Test
    public void shouldGetSourceNameFromSource() {
        for (int i = 0; i != 10; ++i) {
            assertThat(connection.getSourceName(), is(sourceName));
        }
        verify(source, times(10)).getName();
    }

    @Test
    public void shouldGetDefaultCachePolicyFromRepository() {
        assertThat(connection.getDefaultCachePolicy(), is(sameInstance(defaultCachePolicy)));
        verify(repository, times(1)).getDefaultCachePolicy();
    }

    @Test
    public void shouldHaveNoXaResource() {
        assertThat(connection.getXAResource(), is(nullValue()));
    }

    @Test
    public void shouldCheckRepositoryAdminsStartedStateForPingResult() {
        stub(repositoryAdmin.isStarted()).toReturn(true);
        assertThat(connection.ping(1, TimeUnit.SECONDS), is(true));
        verify(repositoryAdmin, times(1)).isStarted();
    }

    @Test( expected = RepositorySourceException.class )
    public void shouldFailExecutionIfRepositoryAdminsIsNotStarted() {
        stub(repositoryAdmin.isStarted()).toReturn(false);
        ExecutionEnvironment env = mock(ExecutionEnvironment.class);
        GraphCommand command = mock(GraphCommand.class);
        connection.execute(env, command);
    }

    @Test
    public void shouldReturnImmediatelyWhenExecutingNullOrEmptyCommandArray() {
        stub(repositoryAdmin.isStarted()).toReturn(true);
        ExecutionEnvironment env = mock(ExecutionEnvironment.class);
        connection.execute(env, (GraphCommand[])null);
        verify(repositoryAdmin, times(1)).isStarted();
        connection.execute(env, new GraphCommand[0]);
        verify(repositoryAdmin, times(2)).isStarted();
    }

    @Test
    public void shouldSkipNullCommandReferencesWhenExecuting() {
        stub(repositoryAdmin.isStarted()).toReturn(true);
        ExecutionEnvironment env = mock(ExecutionEnvironment.class);
        connection.execute(env, new GraphCommand[] {null, null, null});
        verify(repositoryAdmin, times(1)).isStarted();
    }

    @Test
    public void shouldAddListenerToRepositoryWhenSetOnConnection() {
        // Old listener is no-op, so it is not removed from repository ...
        RepositorySourceListener listener = mock(RepositorySourceListener.class);
        connection.setListener(listener);
        verify(repository, times(1)).addListener(listener);

        // Old listener is NOT no-op, so it is removed from repository ...
        RepositorySourceListener listener2 = mock(RepositorySourceListener.class);
        connection.setListener(listener2);
        verify(repository, times(1)).removeListener(listener);
        verify(repository, times(1)).addListener(listener2);
    }

    @Test
    public void shouldRemoveListenerFromRepositoryWhenConnectionIsClosed() {
        // Old listener is NOT no-op, so it is removed from repository ...
        RepositorySourceListener listener2 = mock(RepositorySourceListener.class);
        connection.setListener(listener2);
        verify(repository, times(1)).addListener(listener2);

        // Closing connection will remove listener ...
        connection.close();
        verify(repository, times(1)).removeListener(listener2);
    }
}
