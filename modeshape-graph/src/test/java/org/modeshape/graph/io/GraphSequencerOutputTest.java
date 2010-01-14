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
package org.modeshape.graph.io;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class GraphSequencerOutputTest {
    private GraphSequencerOutput output;
    private ExecutionContext context;
    private Graph graph;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();

        final InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("actual");
        RepositoryContext repositoryContext = new RepositoryContext() {
            @SuppressWarnings( "synthetic-access" )
            public ExecutionContext getExecutionContext() {
                return context;
            }

            public Observer getObserver() {
                return null;
            }

            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return null;
            }

            @SuppressWarnings( "synthetic-access" )
            public Subgraph getConfiguration( int depth ) {
                Graph result = Graph.create(source, context);
                result.useWorkspace("configSpace");
                return result.getSubgraphOfDepth(depth).at("/");
            }
        };
        source.initialize(repositoryContext);

        graph = Graph.create(source, context);

        output = new GraphSequencerOutput(graph);
    }

    protected Path createPath( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected Name createName( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    @Test
    public void shouldSetPropertyWithString() {
        String path1 = "/a";
        String prop1_name = "prop1";
        String value_1 = "blue";
        output.setProperty(path1, prop1_name, value_1);
        String path2 = "/a/b";
        String prop2_name = "prop2";
        String value_2 = "red";
        output.setProperty(path2, prop2_name, value_2);
        output.close();
        Subgraph result = graph.getSubgraphOfDepth(10).at("/");
        String v1 = (String)result.getNode(path1).getProperty(prop1_name).getFirstValue();
        assertThat(v1, is(value_1));
        String v2 = (String)result.getNode(path2).getProperty(prop2_name).getFirstValue();
        assertThat(v2, is(value_2));
        assertNull(result.getNode("/c"));
    }

    @Test
    public void shouldSetReferenceWithString() {
        String path1 = "/a";
        String prop1_name = "prop1";
        String value_1 = "blue";
        output.setReference(path1, prop1_name, value_1);
        String path2 = "/a/b";
        String prop2_name = "prop2";
        String value_2 = "red";
        output.setReference(path2, prop2_name, value_2);
        output.close();
        Subgraph result = graph.getSubgraphOfDepth(10).at("/");
        String v1 = (String)result.getNode(path1).getProperty(prop1_name).getFirstValue();
        assertThat(v1, is(value_1));
        String v2 = (String)result.getNode(path2).getProperty(prop2_name).getFirstValue();
        assertThat(v2, is(value_2));
        assertNull(result.getNode("/c"));
    }

    @Test
    public void shouldSetPropertyWithPath() {
        Path path1 = createPath("/a");
        Name prop1_name = createName("prop1");
        String value_1 = "blue";
        output.setProperty(path1, prop1_name, value_1);
        Path path2 = createPath("/a/b");
        Name prop2_name = createName("prop2");
        String value_2 = "red";
        output.setProperty(path2, prop2_name, value_2);
        output.close();
        Subgraph result = graph.getSubgraphOfDepth(10).at("/");
        String v1 = (String)result.getNode(path1).getProperty(prop1_name).getFirstValue();
        assertThat(v1, is(value_1));
        String v2 = (String)result.getNode(path2).getProperty(prop2_name).getFirstValue();
        assertThat(v2, is(value_2));
        assertNull(result.getNode("/c"));
    }
}
