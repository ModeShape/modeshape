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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionPool;
import org.jboss.dna.spi.graph.connection.RepositorySource;
import org.jboss.dna.spi.graph.connection.RepositorySourceException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederatedSourceTest {

    private FederatedSource source;
    @Mock
    private RepositoryConnection connection;
    @Mock
    private RepositorySource repositorySource;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.repositorySource = mock(RepositorySource.class);
        this.source = new FederatedSource(this.repositorySource);
        this.connection = mock(RepositoryConnection.class);
    }

    @After
    public void afterEach() throws Exception {
        RepositoryConnectionPool pool = source.getConnectionPool();
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldConstructWithNonNullRepositorySource() {
        assertThat(source, is(notNullValue()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToConstructWithNullRepositorySource() {
        new FederatedSource(null);
    }

    @Test
    public void shouldHaveConnectionPoolAfterConstruction() {
        assertThat(source.getConnectionPool(), is(notNullValue()));
    }

    @Test
    public void shouldGetTheNameFromTheRepositorySource() {
        String theName = "My Repository";
        stub(repositorySource.getName()).toReturn(theName);
        assertThat(source.getName(), is(theName));
        verify(repositorySource, times(1)).getName();

        // Change the name of the source, and try again ...
        theName = theName + " part deux";
        stub(repositorySource.getName()).toReturn(theName);
        assertThat(source.getName(), is(theName));
        verify(repositorySource, times(2)).getName();
    }

    @Test
    public void shouldCheckAvailabilityByCreatingConnectionAndPing() throws Exception {
        // Set up the connection mock to return value from ping, and then
        // set up the source mock to return the connection ...
        stub(this.repositorySource.getConnection()).toReturn(connection);
        stub(connection.ping(1, TimeUnit.SECONDS)).toReturn(true);
        assertThat(source.isAvailable(1, TimeUnit.SECONDS), is(true));
    }

    @Test
    public void shouldConsiderSourceToNotBeAvailabilityWhenUnableToPingConnection() throws Exception {
        // Set up the connection mock to return value from ping, and then
        // set up the source mock to return the connection ...
        stub(this.repositorySource.getConnection()).toReturn(connection);
        stub(connection.ping(1, TimeUnit.SECONDS)).toReturn(false);
        assertThat(source.isAvailable(1, TimeUnit.SECONDS), is(false));
    }

    @Test
    public void shouldConsiderSourceToNotBeAvailabilityWhenPingThrowsException() throws Exception {
        // Set up the connection mock to return value from ping, and then
        // set up the source mock to return the connection ...
        stub(this.repositorySource.getConnection()).toReturn(connection);
        stub(connection.ping(1, TimeUnit.SECONDS)).toThrow(new NullPointerException("sorry"));
        assertThat(source.isAvailable(1, TimeUnit.SECONDS), is(false));
    }

    @Test
    public void shouldConsiderSourceToNotBeAvailabilityWhenGetConnectionThrowsRepositorySourceException() throws Exception {
        // Set up the connection mock to return value from ping, and then
        // set up the source mock to return the connection ...
        stub(this.repositorySource.getConnection()).toThrow(new RepositorySourceException("sorry"));
        assertThat(source.isAvailable(1, TimeUnit.SECONDS), is(false));
    }

    @Test
    public void shouldConsiderSourceToNotBeAvailabilityWhenGetConnectionThrowsIllegalStateException() throws Exception {
        stub(this.repositorySource.getConnection()).toThrow(new IllegalStateException("sorry"));
        assertThat(source.isAvailable(1, TimeUnit.SECONDS), is(false));
    }

    @Test
    public void shouldConsiderSourceToNotBeAvailabilityWhenGetConnectionThrowsInterruptedException() throws Exception {
        stub(this.repositorySource.getConnection()).toThrow(new InterruptedException("sorry"));
        assertThat(source.isAvailable(1, TimeUnit.SECONDS), is(false));
    }

    @Test
    public void shouldConsiderSourceToNotBeAvailabilityWhenGetConnectionThrowsAnyException() throws Exception {
        stub(this.repositorySource.getConnection()).toThrow(new NullPointerException("sorry"));
        assertThat(source.isAvailable(1, TimeUnit.SECONDS), is(false));
    }

    @Test
    public void shouldConsiderEqualAnyFederatedSourcesWithSameName() {
        RepositorySource repositorySource2 = mock(RepositorySource.class);
        FederatedSource source2 = new FederatedSource(repositorySource2);

        String theName = "Some name";
        stub(repositorySource.getName()).toReturn(theName);
        stub(repositorySource2.getName()).toReturn(theName);
        assertThat(source.equals(source2), is(true));
    }

    @Test
    public void shouldNotConsiderEqualAnyFederatedSourcesWithDifferentNames() {
        RepositorySource repositorySource2 = mock(RepositorySource.class);
        FederatedSource source2 = new FederatedSource(repositorySource2);

        String theName = "Some name";
        stub(repositorySource.getName()).toReturn(theName + " ");
        stub(repositorySource2.getName()).toReturn(theName);
        assertThat(source.equals(source2), is(false));

        stub(repositorySource.getName()).toReturn(theName.toLowerCase());
        stub(repositorySource2.getName()).toReturn(theName.toUpperCase());
        assertThat(source.equals(source2), is(false));
    }

}
