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
package org.modeshape.graph.connector.test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.modeshape.common.util.IoUtil;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.request.CreateNodeRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * A class that provides standard tests for connectors that check whether a connector correctly updates the content in an
 * 'all-or-nothing' style. This class is designed to be extended for each connector, and in each subclass the
 * {@link #setUpSource()} method is defined to provide a valid {@link RepositorySource} for the connector to be tested.
 * <p>
 * Since these tests do modify repository content, the repository is set up for each test, given each test a pristine repository
 * (as {@link #initializeContent(Graph) initialized} by the concrete test case class).
 * </p>
 */
public abstract class TransactionalConnectorTest extends AbstractConnectorTest {

    protected String[] validLargeValues;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

        // Load in the large value ...
        validLargeValues = new String[] {IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum1.txt")),
            IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum2.txt")),
            IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum3.txt"))};
    }

    @Override
    @After
    public void afterEach() throws Exception {
        super.afterEach();
    }

    /**
     * These tests require that the source supports updates, since all of the tests do some form of updates.
     */
    @Test
    public void shouldHaveUpdateCapabilities() {
        assertThat(source.getCapabilities().supportsUpdates(), is(true));
    }

    protected void setupInitialData() {
        graph.batch()
        // these should all succeed ...
             .create("/a")
             .with("propB", "valueB")
             .and("propC", "valueC")
             .and()
             .create("/a/b")
             .with("propB", "valueB")
             .and("propC", "valueC")
             .and()
             .create("/a/b/c")
             .with("propB", "valueB")
             .and("propC", "valueC")
             .and()
             .create("/a/b/d")
             .with("propB", "valueB")
             .and("propC", "valueC")
             .and()
             .execute();

        // There should have been 3 CreateNodeRequests in one Changes...
        assertThat(allChanges.size(), is(1));
        Changes changes = allChanges.removeFirst();
        assertThat(changes.getChangeRequests().get(0), is(instanceOf(CreateNodeRequest.class)));
        assertThat(changes.getChangeRequests().get(0).changedLocation().getPath(), is(path("/a")));
        assertThat(changes.getChangeRequests().get(1), is(instanceOf(CreateNodeRequest.class)));
        assertThat(changes.getChangeRequests().get(1).changedLocation().getPath(), is(path("/a/b")));
        assertThat(changes.getChangeRequests().get(2), is(instanceOf(CreateNodeRequest.class)));
        assertThat(changes.getChangeRequests().get(2).changedLocation().getPath(), is(path("/a/b/c")));
        assertThat(changes.getChangeRequests().get(3), is(instanceOf(CreateNodeRequest.class)));
        assertThat(changes.getChangeRequests().get(3).changedLocation().getPath(), is(path("/a/b/d")));
    }

    protected void assertInitialData() {

        // Now check that the data is all there ...
        Subgraph subgraph = graph.getSubgraphOfDepth(10).at("/");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode("/a").getProperty("propB").isSingle(), is(true));
        assertThat(subgraph.getNode("/a").getProperty("propB").getFirstValue(), is((Object)"valueB"));
        assertThat(subgraph.getNode("/a").getProperty("propC").isSingle(), is(true));
        assertThat(subgraph.getNode("/a").getProperty("propC").getFirstValue(), is((Object)"valueC"));
        assertThat(subgraph.getNode("/a/b").getProperty("propB").isSingle(), is(true));
        assertThat(subgraph.getNode("/a/b").getProperty("propB").getFirstValue(), is((Object)"valueB"));
        assertThat(subgraph.getNode("/a/b").getProperty("propC").isSingle(), is(true));
        assertThat(subgraph.getNode("/a/b").getProperty("propC").getFirstValue(), is((Object)"valueC"));
        assertThat(subgraph.getNode("/a/b/c").getProperty("propB").isSingle(), is(true));
        assertThat(subgraph.getNode("/a/b/c").getProperty("propB").getFirstValue(), is((Object)"valueB"));
        assertThat(subgraph.getNode("/a/b/c").getProperty("propC").isSingle(), is(true));
        assertThat(subgraph.getNode("/a/b/c").getProperty("propC").getFirstValue(), is((Object)"valueC"));
        assertThat(subgraph.getNode("/a/b/d").getProperty("propB").isSingle(), is(true));
        assertThat(subgraph.getNode("/a/b/d").getProperty("propB").getFirstValue(), is((Object)"valueB"));
        assertThat(subgraph.getNode("/a/b/d").getProperty("propC").isSingle(), is(true));
        assertThat(subgraph.getNode("/a/b/d").getProperty("propC").getFirstValue(), is((Object)"valueC"));
        assertThat(subgraph.getNode("/a").getChildrenSegments().size(), is(1));
        assertThat(subgraph.getNode("/a").getChildrenSegments().get(0), is(segment("b")));
        assertThat(subgraph.getNode("/a/b").getChildrenSegments().size(), is(2));
        assertThat(subgraph.getNode("/a/b").getChildrenSegments().get(0), is(segment("c")));
        assertThat(subgraph.getNode("/a/b").getChildrenSegments().get(1), is(segment("d")));
        assertThat(subgraph.getNode("/a/b/c").getChildrenSegments().isEmpty(), is(true));
        assertThat(subgraph.getNode("/a/b/d").getChildrenSegments().isEmpty(), is(true));
    }

    @Test
    public void shouldMakeChangesWithoutErrors() {
        setupInitialData();
        assertInitialData();
    }

    @Test
    public void shouldBeEmptyAfterCreatingInitialNodesWithFailures() {
        // Create a batch that contains all valid requests except for one ...
        Graph.Batch batch = graph.batch()
                                 .create("/a")
                                 .with("propB", "valueB")
                                 .and("propC", "valueC")
                                 .and()
                                 .create("/a/b")
                                 .with("propB", "valueB")
                                 .and("propC", "valueC")
                                 .and()
                                 .create("/a/b/c")
                                 .with("propB", "valueB")
                                 .and("propC", "valueC")
                                 .and()
                                 // This should fail ...
                                 .create("/x/y/z")
                                 .with("propB", "valueB")
                                 .and("propC", "valueC")
                                 .and()
                                 // This is valid ...
                                 .create("/a/b/cx")
                                 .with("propB", "valueB")
                                 .and("propC", "valueC")
                                 .and();
        try {
            batch.execute();
            fail("Expected to get an error");
        } catch (PathNotFoundException e) {
            // expected ...
        }

        // There should have been no Changes ...
        assertThat(allChanges.isEmpty(), is(true));

        // Check that the root node has no children ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root.getChildren().isEmpty(), is(true));
    }

    @Test
    public void shouldNotChangePersistedStateAfterMakingMultipleChangesWithOneFailure() {
        // Populate the graph with some content, and verify all is well so far ...
        // /a { propB="valueB", propC="valueC" }
        // /a/b { propB="valueB", propC="valueC" }
        // /a/b/c { propB="valueB", propC="valueC" }
        // /a/b/d { propB="valueB", propC="valueC" }
        setupInitialData();
        assertInitialData();

        // Now create a batch that has 2 good requests and 1 bad request ...
        Graph.Batch batch = graph.batch().create("/a/x") // good
                                 .with("propD", "valueD")
                                 .and("propE", "valueE")
                                 .and()
                                 .create("/a/n/m")
                                 // BAD
                                 .with("propF", "valueF")
                                 .and("propG", "valueG")
                                 .and()
                                 .create("/a/b/e")
                                 // good
                                 .with("propH", "valueH")
                                 .and("propC", "valueC")
                                 .and();
        try {
            batch.execute();
            fail("Expected to get an error");
        } catch (PathNotFoundException e) {
            // expected ...
        }

        // There should have been no Changes ...
        assertThat(allChanges.isEmpty(), is(true));

        // The initial data should still be there ...
        assertInitialData();
    }
}
