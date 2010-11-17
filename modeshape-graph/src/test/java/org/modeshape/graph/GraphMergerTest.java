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
package org.modeshape.graph;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.net.URL;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.connector.xmlfile.XmlFileRepositorySource;
import org.modeshape.graph.property.PathNotFoundException;

/**
 * 
 */
public class GraphMergerTest {

    private ExecutionContext context;
    private InMemoryRepositorySource graphSource;
    private XmlFileRepositorySource initialContentSource;
    private Graph graph;
    private Graph initialContent;
    private boolean print = false;

    @Before
    public void beforeEaach() {
        context = new ExecutionContext();

        graphSource = new InMemoryRepositorySource();
        graphSource.setName("Graph source");

        initialContentSource = new XmlFileRepositorySource();
        initialContentSource.setName("Initial content source");
        initialContentSource.setContentLocation(resourceUrl("aircraft.xml"));

        graph = Graph.create(graphSource, context);
        initialContent = Graph.create(initialContentSource, context);
    }

    @Test
    public void shouldHaveNonEmptyInitialContent() {
        assertNodeExists(initialContent, "Aircraft");
    }

    @Test
    public void shouldInitializeEmptySource() {
        assertNoChildren(graph, "/");
        graph.merge(initialContent);
        // print = true;
        printSubgraph(graph, "/");
        assertNodeExists(initialContent, "Aircraft");
        assertNodeDoesNotExist(initialContent, "Aircraft[2]");
    }

    @Test
    public void shouldInitializeSourceWithOnlySomeNodes() {
        assertNoChildren(graph, "/");
        graph.create("/Aircraft").and();
        graph.merge(initialContent);
        // print = true;
        printSubgraph(graph, "/");
        assertNodeExists(initialContent, "Aircraft");
        assertNodeDoesNotExist(initialContent, "Aircraft[2]");
    }

    protected void assertNoChildren( Graph graph,
                                     String path ) {
        assertNodeExists(graph, path);
        List<Location> children = graph.getChildren().of(path);
        assertThat(children.isEmpty(), is(true));
    }

    protected Node assertNodeExists( Graph graph,
                                     String path ) {
        Node node = graph.getNodeAt(path);
        assertThat(node, is(notNullValue()));
        return node;
    }

    protected void assertNodeDoesNotExist( Graph graph,
                                           String path ) {
        try {
            graph.getNodeAt(path);
            fail("Node does exist at \"" + path + "\"");
        } catch (PathNotFoundException e) {
            // expected ...
        }
    }

    protected void printSubgraph( Graph graph,
                                  String path ) {
        if (print) {
            Subgraph subgraph = graph.getSubgraphOfDepth(Integer.MAX_VALUE).at(path);
            System.out.println(subgraph);
        }
    }

    protected String resourceUrl( String path ) {
        URL url = this.getClass().getClassLoader().getResource(path);
        return url != null ? url.toExternalForm() : path;
    }

}
