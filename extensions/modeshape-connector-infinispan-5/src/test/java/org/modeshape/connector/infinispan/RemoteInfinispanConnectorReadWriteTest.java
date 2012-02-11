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
package org.modeshape.connector.infinispan;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryContext;

/**
 * @author johnament
 */
public class RemoteInfinispanConnectorReadWriteTest {

    private ExecutionContext context;
    private RemoteInfinispanSource source;
    private RepositoryContext mockRepositoryContext;
    private Graph graph;

    @Before
    public void beforeEach() throws Exception {
        RemoteInfinispanTestHelper.createServer();

        context = new ExecutionContext();
        mockRepositoryContext = mock(RepositoryContext.class);
        when(mockRepositoryContext.getExecutionContext()).thenReturn(context);
        String[] predefinedWorkspaceNames = new String[] {"default"};
        source = new RemoteInfinispanSource();
        source.setName("Test Repository");
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(true);
        source.initialize(mockRepositoryContext);
        source.setRemoteInfinispanServerList(String.format("%s:%s",
                                                           RemoteInfinispanTestHelper.hostAddress(),
                                                           RemoteInfinispanTestHelper.hostPort()));
    }

    @After
    public void afterEach() throws Exception {
        graph = null;
        try {
            source.close(); // stops the cache manager
        } finally {
            RemoteInfinispanTestHelper.releaseServer();
        }
    }

    protected Graph graph() {
        if (graph == null) {
            graph = Graph.create(source, context);
        }
        return graph;
    }

    private void testWriteAndRead() {
        Subgraph subgraph = graph().getSubgraphOfDepth(10).at("/");
        assertThat(subgraph.getNode("/"), is(notNullValue()));
        // System.out.println(subgraph);
        graph().create("/a").with("prop1", "value1").and();
        subgraph = graph().getSubgraphOfDepth(10).at("/");
        assertThat(subgraph.getNode("/"), is(notNullValue()));
        assertThat(subgraph.getNode("/a").getProperty("prop1").getFirstValue(), is((Object)"value1"));
        // System.out.println(subgraph);
    }

    @Test
    public void shouldShutdownWithoutOpeningConnections() throws Exception {
        source.close();
    }

    @Test
    public void shouldShutdownAfterOpeningConnections() throws Exception {
        RepositoryConnection connection = source.getConnection();
        connection.close();
    }

    @Test
    public void shouldHaveRootNode() throws Exception {
        assertThat(graph().getNodeAt("/"), is(notNullValue()));
    }

    @Test
    public void shouldAllowCreatingAndReReadingNodesFromJndiCache() throws Exception {
        testWriteAndRead();
    }

    @Test
    public void shouldAllowCreatingAndReReadingNodesFromClasspathConfig() throws Exception {
        testWriteAndRead();
    }

    @Test
    public void shouldAllowCreatingAndReReadingNodesFromFileConfig() throws Exception {
        testWriteAndRead();
    }

}
