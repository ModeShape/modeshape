/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.connector.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.request.CompositeRequest;
import org.jboss.dna.graph.request.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederatedRepositoryConnectionTest {

    private FederatedRepositoryConnection connection;
    @Mock
    private FederatedRepository repository;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        connection = new FederatedRepositoryConnection(repository, null);
    }

    @Test
    public void shouldHaveRepository() {
        assertThat(connection.getRepository(), is(repository));
    }

    @Test
    public void shouldHaveNoXaResource() {
        assertThat(connection.getXAResource(), is(nullValue()));
    }

    @Test
    public void shouldGetDefaultCachePolicyFromRepository() {
        CachePolicy cachePolicy = mock(CachePolicy.class);
        stub(repository.getDefaultCachePolicy()).toReturn(cachePolicy);
        assertThat(connection.getDefaultCachePolicy(), is(cachePolicy));
        verify(repository, times(1)).getDefaultCachePolicy();
    }

    @Test
    public void shouldGetSourceNameFromRepository() {
        String name = "Something";
        stub(repository.getSourceName()).toReturn(name);
        assertThat(connection.getSourceName(), is(name));
        verify(repository, times(1)).getSourceName();
    }

    @Test
    public void shouldProcessNonCompositeRequestSynchronously() {
        Request request = mock(Request.class);
        assertThat(connection.shouldProcessSynchronously(request), is(true));
    }

    @Test
    public void shouldProcessCompositeRequestWithOneRequestSynchronously() {
        CompositeRequest request = mock(CompositeRequest.class);
        stub(request.size()).toReturn(1);
        assertThat(connection.shouldProcessSynchronously(request), is(true));
    }

    @Test
    public void shouldProcessCompositeRequestWithMultipleRequestsAsynchronously() {
        CompositeRequest request = mock(CompositeRequest.class);
        stub(request.size()).toReturn(2);
        assertThat(connection.shouldProcessSynchronously(request), is(false));
    }

    @Test
    public void shouldAllowCloseToBeCalledRepeatedly() {
        for (int i = 0; i != 10; ++i) {
            connection.close();
        }
    }
}
