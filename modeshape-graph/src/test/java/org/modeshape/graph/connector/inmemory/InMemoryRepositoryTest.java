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
package org.modeshape.graph.connector.inmemory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.RepositoryContext;

public class InMemoryRepositoryTest {

    private InMemoryRepositorySource source;
    private RepositoryContext repositoryContext;
    private ExecutionContext context;

    @Before
    public void beforeEach() throws Exception {
        context = new ExecutionContext();
        repositoryContext = mock(RepositoryContext.class);
        when(repositoryContext.getExecutionContext()).thenReturn(context);
        source = new InMemoryRepositorySource();
        source.setName("SomeName");
        source.setRootNodeUuid(UUID.randomUUID());
        source.initialize(repositoryContext);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullSourceInConstructor() {
        new InMemoryRepository(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowSourceWithBlankNameInConstructor() {
        source.setName("   \t ");
        new InMemoryRepository(source);
    }

    @Test
    public void shouldAllowValidSourceInConstructor() {
        new InMemoryRepository(source);
    }
}
