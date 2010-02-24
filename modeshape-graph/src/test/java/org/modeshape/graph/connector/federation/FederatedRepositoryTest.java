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
package org.modeshape.graph.connector.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

/**
 * @author Randall Hauch
 */
public class FederatedRepositoryTest {

    private String name;
    private FederatedRepository repository;
    private CachePolicy defaultCachePolicy;
    private ExecutorService executor;
    @Mock
    private FederatedWorkspace workspace1;
    @Mock
    private FederatedWorkspace workspace2;
    @Mock
    private RepositoryConnectionFactory connectionFactory;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        name = "Repository";
        executor = Executors.newSingleThreadExecutor();
        when(workspace1.getName()).thenReturn("workspace1");
        when(workspace2.getName()).thenReturn("workspace2");
        Collection<FederatedWorkspace> configs = new ArrayList<FederatedWorkspace>();
        configs.add(workspace1);
        configs.add(workspace2);
        repository = new FederatedRepository(name, connectionFactory, configs, defaultCachePolicy, executor);
    }

    @Test
    public void shouldHaveNameUponCreation() {
        assertThat(repository.getSourceName(), is(name));
    }

    @Test
    public void shouldFindExistingWorkspaces() {
        assertThat(repository.getWorkspace("workspace1"), is(sameInstance(workspace1)));
        assertThat(repository.getWorkspace("workspace2"), is(sameInstance(workspace2)));
    }

    @Test
    public void shouldFindDefaultWorkspaceIfThereIsOne() {
        assertThat(repository.getWorkspace(null), is(sameInstance(workspace1)));
    }

    @Test( expected = InvalidWorkspaceException.class )
    public void shouldNotFindWorkspaceThatDoesNotExist() {
        repository.getWorkspace("non-existant");
    }
}
