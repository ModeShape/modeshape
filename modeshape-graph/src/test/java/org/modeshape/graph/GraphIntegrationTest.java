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
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.Graph.Batch;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.request.FunctionRequest;
import org.modeshape.graph.request.ReadBranchRequest;
import org.modeshape.graph.request.function.Function;
import org.modeshape.graph.request.function.FunctionContext;
import org.xml.sax.SAXException;

public class GraphIntegrationTest {

    private ExecutionContext context;
    private InMemoryRepositorySource graphSource;
    private Graph graph;
    private boolean print = false;

    @Before
    public void beforeEaach() throws SAXException, IOException {
        context = new ExecutionContext();

        graphSource = new InMemoryRepositorySource();
        graphSource.setName("Graph source");

        graph = Graph.create(graphSource, context);
        graph.importXmlFrom(resourceUrl("aircraft.xml")).into("/");
    }

    @Test
    public void shouldHaveNonEmptyInitialContent() {
        assertNodeExists(graph, "Aircraft");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Apply functions immediately ...
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldSuccessfullyApplyFunction() {
        // Create some nodes and get the subgraph of them ...
        Subgraph subgraph = graph.getSubgraphOfDepth(100).at("/");
        int count = countNodes(subgraph);

        // Determine the number of nodes using a function ...
        Map<String, Serializable> output = graph.applyFunction(new CountNodesFunction()).to("/");

        assertThat(output, is(notNullValue()));
        assertThat(output.get("nodeCount"), is((Object)new Integer(count)));
    }

    @Test( expected = IllegalStateException.class )
    public void shouldResultInExceptionIfFunctionResultsInError() {
        graph.applyFunction(new SetErrorFunction()).to("/");
    }

    @Test( expected = IllegalStateException.class )
    public void shouldResultInExceptionIfFunctionThrowsException() {
        graph.applyFunction(new ThrowErrorFunction()).to("/");
    }

    @Test
    public void shouldAllowSpecifyingOneInputParametersWhenApplyingFunction() {
        // Create some nodes and get the subgraph of them ...
        Subgraph subgraph = graph.getSubgraphOfDepth(100).at("/");
        int count = countNodes(subgraph);

        Function function = new CountNodesFunction();
        Map<String, Serializable> output = graph.applyFunction(function).withInput("a", "value").to("/");
        assertThat(output.get("nodeCount"), is((Object)new Integer(count)));
    }

    @Test
    public void shouldAllowSpecifyingMultipleInputParametersWhenApplyingFunction() {
        // Create some nodes and get the subgraph of them ...
        Subgraph subgraph = graph.getSubgraphOfDepth(100).at("/");
        int count = countNodes(subgraph);

        Function function = new CountNodesFunction();
        Map<String, Serializable> output = graph.applyFunction(function).withInput("a", "value").and("b", "foo").to("/");
        assertThat(output.get("nodeCount"), is((Object)new Integer(count)));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Apply functions in batch ...
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldSuccessfullyApplyFunctionUsingBatch() {
        // Create some nodes and get the subgraph of them ...
        Subgraph subgraph = graph.getSubgraphOfDepth(100).at("/");
        int count = countNodes(subgraph);

        // Determine the number of nodes using a function ...
        Results results = graph.batch().applyFunction(new CountNodesFunction()).to("/").execute();

        assertThat(results, is(notNullValue()));
        assertThat(results.getRequests().size(), is(1));
        FunctionRequest request = (FunctionRequest)results.getRequests().get(0);
        assertThat(request.output("nodeCount", PropertyType.LONG, (Long)null, context), is(new Long(count)));
    }

    @Test( expected = IllegalStateException.class )
    public void shouldResultInExceptionIfFunctionResultsInErrorUsingBatch() {
        graph.batch().applyFunction(new SetErrorFunction()).to("/").execute();
    }

    @Test( expected = IllegalStateException.class )
    public void shouldResultInExceptionIfFunctionThrowsExceptionUsingBatch() {
        graph.batch().applyFunction(new ThrowErrorFunction()).to("/").execute();
    }

    @Test
    public void shouldAllowSpecifyingOneInputParametersWhenApplyingFunctionUsingBatch() {
        Function function = new ThrowErrorFunction();
        Batch batch = graph.batch().applyFunction(function).withInput("a", "value").to("/");
        assertThat(batch.isExecuteRequired(), is(true));
    }

    @Test
    public void shouldAllowSpecifyingMultipleInputParametersWhenApplyingFunctionUsingBatch() {
        Function function = new ThrowErrorFunction();
        Batch batch = graph.batch().applyFunction(function).withInput("a", "value").and("b", "foo").to("/");
        assertThat(batch.isExecuteRequired(), is(true));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Function implementations ...
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Counts the nodes in a subgraph at and below the node at which the function is applied, and place the number in the
     * "nodeCount" output parameter. The optional input parameter "maxDepth" can be used to specify the maximum depth.
     */
    protected static class CountNodesFunction extends Function {
        private static final long serialVersionUID = 1L;

        @Override
        public void run( FunctionContext context ) {
            // Read the input parameter(s) ...
            int maxDepth = context.input("maxDepth", PropertyType.LONG, new Long(Integer.MAX_VALUE)).intValue();

            // Read the subgraph under the location ...
            ReadBranchRequest readSubgraph = context.builder().readBranch(context.appliedAt(), context.workspace(), maxDepth);
            // Process that request ...
            if (readSubgraph.hasError()) {
                readSubgraph.getError().printStackTrace();
                context.setError(readSubgraph.getError());
            } else {

                // And count the number of nodes within the subgraph ...
                int counter = 0;
                for (Location location : readSubgraph) {
                    if (location != null) ++counter;
                }

                // And write the count as an output parameter ...
                context.setOutput("nodeCount", counter);
            }
        }
    }

    /**
     * Always sets an error on the request.
     */
    protected static class SetErrorFunction extends Function {
        private static final long serialVersionUID = 1L;

        @Override
        public void run( FunctionContext context ) {
            context.setError(new IllegalStateException("Bogus exceptions"));
        }
    }

    /**
     * Always just throws an exception.
     */
    protected static class ThrowErrorFunction extends Function {
        private static final long serialVersionUID = 1L;

        @Override
        public void run( FunctionContext context ) {
            throw new IllegalStateException("Bogus exceptions");
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility methods ...
    // ----------------------------------------------------------------------------------------------------------------

    protected int countNodes( Subgraph subgraph ) {
        return countNodes(subgraph.getRoot(), subgraph);
    }

    protected int countNodes( SubgraphNode node,
                              Subgraph subgraph ) {
        assertThat(node, is(notNullValue()));
        assertThat(subgraph, is(notNullValue()));
        int count = 1;
        for (Location child : node.getChildren()) {
            count += countNodes(subgraph.getNode(child), subgraph);
        }
        return count;
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
