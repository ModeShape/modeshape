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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNoException;
import static org.modeshape.graph.IsNodeWithChildren.hasChild;
import static org.modeshape.graph.IsNodeWithChildren.hasChildren;
import static org.modeshape.graph.IsNodeWithChildren.isEmpty;
import static org.modeshape.graph.IsNodeWithProperty.hasProperty;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.IoUtil;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.UuidAlreadyExistsException;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.Reference;

/**
 * A class that provides standard writing verification tests for connectors that are able to store any content (in any structure).
 * This class is designed to be extended for each connector, and in each subclass the {@link #setUpSource()} method is defined to
 * provide a valid {@link RepositorySource} for the connector to be tested.
 * <p>
 * Since these tests do modify repository content, the repository is set up for each test, given each test a pristine repository
 * (as {@link #initializeContent(Graph) initialized} by the concrete test case class).
 * </p>
 */
public abstract class WritableConnectorTest extends AbstractConnectorTest {

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

    @Test
    public void shouldSetPropertyOnRootNode() {
        graph.set("propA").to("valueA").on("/");
        // Now look up the node ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(ModeShapeLexicon.UUID, getRootNodeUuid()));
        assertThat(root, hasProperty("propA", "valueA"));
    }

    @Test
    public void shouldAddChildUnderRootNode() {
        graph.batch().create("/a").with("propB", "valueB").and("propC", "valueC").and().execute();
        // Now look up the root node ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(ModeShapeLexicon.UUID, getRootNodeUuid()));
        assertThat(root.getChildren(), hasChild(segment("a")));

        // Now look up node A ...
        Node nodeA = graph.getNodeAt("/a");
        assertThat(nodeA, is(notNullValue()));
        assertThat(nodeA, hasProperty("propB", "valueB"));
        assertThat(nodeA, hasProperty("propC", "valueC"));
        assertThat(nodeA.getChildren(), isEmpty());
    }

    @Test
    public void shouldAddChildrenAndSettingProperties() {
        graph.batch()
             .set("propA")
             .to("valueA")
             .on("/")
             .and()
             .create("/a")
             .with("propB", "valueB")
             .and("propC", "valueC")
             .and()
             .create("/b")
             .with("propD", "valueD")
             .and("propE", "valueE")
             .and()
             .execute();
        // Now look up the root node ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(ModeShapeLexicon.UUID, getRootNodeUuid()));
        assertThat(root, hasProperty("propA", "valueA"));
        assertThat(root.getChildren(), hasChildren(segment("a"), segment("b")));
        
        // Now look up node A ...
        Node nodeA = graph.getNodeAt("/a");
        assertThat(nodeA, is(notNullValue()));
        assertThat(nodeA, hasProperty("propB", "valueB"));
        assertThat(nodeA, hasProperty("propC", "valueC"));
        assertThat(nodeA.getChildren(), isEmpty());

        // Now look up node B ...
        Node nodeB = graph.getNodeAt("/b");
        assertThat(nodeB, is(notNullValue()));
        assertThat(nodeB, hasProperty("propD", "valueD"));
        assertThat(nodeB, hasProperty("propE", "valueE"));
        assertThat(nodeB.getChildren(), isEmpty());

        // Get the subgraph ...
        Subgraph subgraph = graph.getSubgraphOfDepth(3).at("/");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode("."), hasProperty(ModeShapeLexicon.UUID, getRootNodeUuid()));
        assertThat(subgraph.getNode("."), hasProperty("propA", "valueA"));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(segment("a"), segment("b")));
        assertThat(subgraph.getNode("a"), is(notNullValue()));
        assertThat(subgraph.getNode("a"), hasProperty("propB", "valueB"));
        assertThat(subgraph.getNode("a"), hasProperty("propC", "valueC"));
        assertThat(subgraph.getNode("a").getChildren(), isEmpty());
        assertThat(subgraph.getNode("b"), is(notNullValue()));
        assertThat(subgraph.getNode("b"), hasProperty("propD", "valueD"));
        assertThat(subgraph.getNode("b"), hasProperty("propE", "valueE"));
        assertThat(subgraph.getNode("b").getChildren(), isEmpty());
    }

    @Test
    public void shouldStoreManyPropertiesOnANode() {
        Graph.Create<Graph.Batch> create = graph.batch().create("/a");
        for (int i = 0; i != 100; ++i) {
            create = create.with("property" + i, "value" + i);
        }
        create.and().execute();
        // Now look up all the properties ...
        Node nodeA = graph.getNodeAt("/a");
        assertThat(nodeA, is(notNullValue()));
        for (int i = 0; i != 100; ++i) {
            assertThat(nodeA, hasProperty("property" + i, "value" + i));
        }
        assertThat(nodeA.getChildren(), isEmpty());
    }

    @Test
    public void shouldUpdateSmallPropertiesOnANode() {
        // Create the property and add some properties (including 2 large values) ...
        Graph.Create<Graph.Batch> create = graph.batch().create("/a");
        for (int i = 0; i != 10; ++i) {
            create = create.with("property" + i, "value" + i);
        }
        create.and().execute();

        // Now look up all the properties ...
        Node nodeA = graph.getNodeAt("/a");
        assertThat(nodeA, is(notNullValue()));
        for (int i = 0; i != 10; ++i) {
            assertThat(nodeA, hasProperty("property" + i, "value" + i));
        }
        assertThat(nodeA.getChildren(), isEmpty());

        // Now, remove some of the properties and add some others ...
        Graph.Batch batch = graph.batch();
        batch.remove("property0", "property1").on("/a");
        batch.set("property6").to("new valid 6").on("/a");
        batch.execute();

        // Re-read the properties ...
        nodeA = graph.getNodeAt("/a");
        assertThat(nodeA, is(notNullValue()));
        for (int i = 0; i != 10; ++i) {
            if (i == 0 || i == 1) {
                continue;
            } else if (i == 6) {
                assertThat(nodeA, hasProperty("property" + i, "new valid 6"));
            } else {
                assertThat(nodeA, hasProperty("property" + i, "value" + i));
            }
        }
        assertThat(nodeA.getChildren(), isEmpty());

    }

    @Test
    public void shouldUpdateLargePropertiesOnANode() {
        // Create the property and add some properties (including 2 large values) ...
        Graph.Create<Graph.Batch> create = graph.batch().create("/a");
        for (int i = 0; i != 100; ++i) {
            create = create.with("property" + i, "value" + i);
        }
        create = create.with("largeProperty1", validLargeValues[0]);
        create = create.with("largeProperty2", validLargeValues[1]);
        create.and().execute();

        // Now look up all the properties ...
        Node nodeA = graph.getNodeAt("/a");
        assertThat(nodeA, is(notNullValue()));
        for (int i = 0; i != 100; ++i) {
            assertThat(nodeA, hasProperty("property" + i, "value" + i));
        }
        assertThat(nodeA, hasProperty("largeProperty1", validLargeValues[0]));
        assertThat(nodeA, hasProperty("largeProperty2", validLargeValues[1]));
        assertThat(nodeA.getChildren(), isEmpty());

        // Now, remove some of the properties and add some others ...
        Graph.Batch batch = graph.batch();
        batch.remove("largeProperty1", "property0", "property1").on("/a");
        batch.set("property50").to("new valid 50").on("/a");
        batch.set("largeProperty3").to(validLargeValues[2]).on("/a");
        batch.execute();

        // Re-read the properties ...
        nodeA = graph.getNodeAt("/a");
        assertThat(nodeA, is(notNullValue()));
        for (int i = 0; i != 100; ++i) {
            if (i == 0 || i == 1) {
                continue;
            } else if (i == 50) {
                assertThat(nodeA, hasProperty("property" + i, "new valid 50"));
            } else {
                assertThat(nodeA, hasProperty("property" + i, "value" + i));
            }
        }
        assertThat(nodeA, hasProperty("largeProperty2", validLargeValues[1]));
        assertThat(nodeA, hasProperty("largeProperty3", validLargeValues[2]));
        assertThat(nodeA.getChildren(), isEmpty());

    }

    @Test
    public void shouldCreateDeepBranchUsingIndividualRequests() {
        String initialPath = "";
        int depth = 50;
        int numChildrenPerNode = 1;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = false;
        String description = "deep and narrow tree, 1x50";
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, description);
    }

    @Test
    public void shouldCreateDeepBranchUsingOneBatch() {
        String initialPath = "";
        int depth = 50;
        int numChildrenPerNode = 1;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        String description = "deep and narrow tree, 1x50";
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, description);
    }

    @Test
    public void shouldCreateFlatAndWideTreeUsingOneBatch() {
        String initialPath = "";
        int depth = 1;
        int numChildrenPerNode = 300;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);
    }

    @Test
    public void shouldCreateBinaryTreeUsingOneBatch() {
        String initialPath = "";
        int depth = 8;
        int numChildrenPerNode = 2;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        String description = "binary tree, 2x8";
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, description);
    }

    @Test
    public void shouldCreateTreeWith10ChildrenAnd2LevelsDeepUsingIndividualRequests() {
        String initialPath = "";
        int depth = 2;
        int numChildrenPerNode = 10;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = false;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);
    }

    @Test
    public void shouldCreateTreeWith10ChildrenAnd2LevelsDeepUsingOneBatch() {
        String initialPath = "";
        int depth = 2;
        int numChildrenPerNode = 10;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);
    }

    @Test
    public void shouldCreateTreeWith10ChildrenAnd3LevelsDeepUsingOneBatch() {
        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 10;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);
    }

    @Test
    public void shouldCreateAndReadTree() {
        // Create the tree (at total of 40 nodes, plus the extra 6 added later)...
        // /
        // /node1
        // /node1/node1
        // /node1/node1/node1
        // /node1/node1/node2
        // /node1/node1/node3
        // /node1/node2
        // /node1/node2/node1
        // /node1/node2/node2
        // /node1/node2/node3
        // /node1/node3
        // /node1/node3/node1
        // /node1/node3/node2
        // /node1/node3/node3
        // /node2
        // /node2/node1
        // /node2/node1/node1
        // /node2/node1/node2
        // /node2/node1/node3
        // /node2/node2
        // /node2/node2/node1
        // /node2/node2/node2
        // /node2/node2/node3
        // /node2/node3
        // /node2/node3/node1
        // /node2/node3/node2
        // /node2/node3/node3
        // /node3
        // /node3/node1
        // /node3/node1/node1
        // /node3/node1/node2
        // /node3/node1/node3
        // /node3/node2
        // /node3/node2/node1
        // /node3/node2/node2
        // /node3/node2/node3
        // /node3/node3
        // /node3/node3/node1
        // /node3/node3/node2
        // /node3/node3/node3
        // /secondBranch1
        // /secondBranch1/secondBranch1
        // /secondBranch1/secondBranch2
        // /secondBranch2
        // /secondBranch2/secondBranch1
        // /secondBranch2/secondBranch2

        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        assertThat(graph.getChildren().of("/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3/node1"), isEmpty());

        assertThat(graph.getChildren().of("/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node2/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node2/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node2/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node2/node3/node1"), isEmpty());

        assertThat(graph.getChildren().of("/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3/node1"), isEmpty());

        Subgraph subgraph = graph.getSubgraphOfDepth(2).at("/node3/node2");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("."), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node1"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node1"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node1"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));

        subgraph = graph.getSubgraphOfDepth(2).at("/node3");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("."), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node1"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node1"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node1"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
    }

    @Test
    public void shouldDeleteNodes() {
        // Create the tree (at total of 40 nodes, plus the extra 6 added later)...
        // /
        // /node1
        // /node1/node1
        // /node1/node1/node1
        // /node1/node1/node2
        // /node1/node1/node3
        // /node1/node2
        // /node1/node2/node1
        // /node1/node2/node2
        // /node1/node2/node3
        // /node1/node3
        // /node1/node3/node1
        // /node1/node3/node2
        // /node1/node3/node3
        // /node2
        // /node2/node1
        // /node2/node1/node1
        // /node2/node1/node2
        // /node2/node1/node3
        // /node2/node2
        // /node2/node2/node1
        // /node2/node2/node2
        // /node2/node2/node3
        // /node2/node3
        // /node2/node3/node1
        // /node2/node3/node2
        // /node2/node3/node3
        // /node3
        // /node3/node1
        // /node3/node1/node1
        // /node3/node1/node2
        // /node3/node1/node3
        // /node3/node2
        // /node3/node2/node1
        // /node3/node2/node2
        // /node3/node2/node3
        // /node3/node3
        // /node3/node3/node1
        // /node3/node3/node2
        // /node3/node3/node3
        // /secondBranch1
        // /secondBranch1/secondBranch1
        // /secondBranch1/secondBranch2
        // /secondBranch2
        // /secondBranch2/secondBranch1
        // /secondBranch2/secondBranch2

        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        // Delete two branches ...
        graph.delete("/node2/node2").and().delete("/node3/node1");

        assertThat(graph.getChildren().of("/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3/node1"), hasChildren());

        assertThat(graph.getChildren().of("/node2"), hasChildren(segment("node1"), segment("node3")));
        assertThat(graph.getChildren().of("/node2/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node2/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node2/node3/node1"), hasChildren());

        assertThat(graph.getChildren().of("/node3"), hasChildren(segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3/node1"), hasChildren());

        Subgraph subgraph = graph.getSubgraphOfDepth(3).at("/");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(segment("node1"), segment("node3")));
        assertThat(subgraph.getNode("node1").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("node1"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node1"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node1"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2").getChildren(), hasChildren(segment("node1"), segment("node3")));
        assertThat(subgraph.getNode("node2"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3").getChildren(), hasChildren(segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("node3"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));

        subgraph = graph.getSubgraphOfDepth(2).at("/node3");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("."), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
    }

    @Test
    public void shouldCopyNodeWithChildren() {
        // Create the tree (at total of 40 nodes, plus the extra 6 added later)...
        // /
        // /node1
        // /node1/node1
        // /node1/node1/node1
        // /node1/node1/node2
        // /node1/node1/node3
        // /node1/node2
        // /node1/node2/node1
        // /node1/node2/node2
        // /node1/node2/node3
        // /node1/node3
        // /node1/node3/node1
        // /node1/node3/node2
        // /node1/node3/node3
        // /node2
        // /node2/node1
        // /node2/node1/node1
        // /node2/node1/node2
        // /node2/node1/node3
        // /node2/node2
        // /node2/node2/node1
        // /node2/node2/node2
        // /node2/node2/node3
        // /node2/node3
        // /node2/node3/node1
        // /node2/node3/node2
        // /node2/node3/node3
        // /node3
        // /node3/node1
        // /node3/node1/node1
        // /node3/node1/node2
        // /node3/node1/node3
        // /node3/node2
        // /node3/node2/node1
        // /node3/node2/node2
        // /node3/node2/node3
        // /node3/node3
        // /node3/node3/node1
        // /node3/node3/node2
        // /node3/node3/node3
        // /secondBranch1
        // /secondBranch1/secondBranch1
        // /secondBranch1/secondBranch2
        // /secondBranch2
        // /secondBranch2/secondBranch1
        // /secondBranch2/secondBranch2

        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        if (source.getCapabilities().supportsReferences()) {
            // Create some references between nodes that aren't involved with the copy ...
            graph.set("refProp").on("/node1").to(graph.getNodeAt("/node1/node3"));
            graph.set("refProp").on("/node1/node1").to(graph.getNodeAt("/node3/node2")); // will soon be /node3/node2[1]

            // Create some "inward" references from nodes that are NOT being copied to nodes that are being copied ...
            graph.set("refProp").on("/node1/node2").to(graph.getNodeAt("/node2/node2"));
            graph.set("refProp").on("/node1/node3").to(graph.getNodeAt("/node2/node2"));

            // Create some "outward" references from nodes that are being copied to nodes that are NOT being copied ...
            graph.set("refProp").on("/node2/node1").to(graph.getNodeAt("/node1/node1"));
            graph.set("refProp").on("/node2/node3").to(graph.getNodeAt("/node1/node2"));

            // Create some "internal" references between nodes that are being copied ...
            graph.set("refProp").on("/node2/node2").to(graph.getNodeAt("/node2/node2/node1"));
            graph.set("refProp").on("/node2/node3/node1").to(graph.getNodeAt("/node2/node2/node1"));

            // Verify the references are there ...
            assertReference("/node1", "refProp", "/node1/node3");
            assertReference("/node1/node1", "refProp", "/node3/node2");
            assertReference("/node1/node2", "refProp", "/node2/node2");
            assertReference("/node1/node3", "refProp", "/node2/node2");
            assertReference("/node2/node1", "refProp", "/node1/node1");
            assertReference("/node2/node3", "refProp", "/node1/node2");
            assertReference("/node2/node2", "refProp", "/node2/node2/node1");
            assertReference("/node2/node3/node1", "refProp", "/node2/node2/node1");
        }

        // Copy a branches ...
        graph.copy("/node2").into("/node3");

        if (source.getCapabilities().supportsReferences()) {
            // Verify the references are still there ...
            assertReference("/node1", "refProp", "/node1/node3");
            assertReference("/node1/node1", "refProp", "/node3/node2[1]");
            assertReference("/node1/node2", "refProp", "/node2/node2");
            assertReference("/node1/node3", "refProp", "/node2/node2");
            assertReference("/node2/node1", "refProp", "/node1/node1");
            assertReference("/node2/node3", "refProp", "/node1/node2");
            assertReference("/node2/node2", "refProp", "/node2/node2/node1");
            assertReference("/node2/node3/node1", "refProp", "/node2/node2/node1");

            // And verify that we have a few new (outward and internal) references in the copy ...
            assertReference("/node3/node2[2]/node1", "refProp", "/node1/node1"); // outward
            assertReference("/node3/node2[2]/node3", "refProp", "/node1/node2"); // outward
            assertReference("/node3/node2[2]/node2", "refProp", "/node3/node2[2]/node2/node1"); // internal
            assertReference("/node3/node2[2]/node3/node1", "refProp", "/node3/node2[2]/node2/node1"); // internal
        }

        // Now assert the structure ...
        assertThat(graph.getChildren().of("/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3/node1"), hasChildren());

        // The original of the copy should still exist ...
        assertThat(graph.getChildren().of("/node2"), hasChildren(segment("node1"), segment("node3")));
        assertThat(graph.getChildren().of("/node2/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node2/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node2/node3/node1"), hasChildren());

        assertThat(graph.getChildren().of("/node3"), hasChildren(segment("node2[1]"), segment("node3"), segment("node2[2]")));
        assertThat(graph.getChildren().of("/node3/node2[1]"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3/node1"), hasChildren());
        assertThat(graph.getChildren().of("/node3/node2[2]"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[2]/node1"), hasChildren(segment("node1"),
                                                                                segment("node2"),
                                                                                segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[2]/node2"), hasChildren(segment("node1"),
                                                                                segment("node2"),
                                                                                segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[2]/node3"), hasChildren(segment("node1"),
                                                                                segment("node2"),
                                                                                segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[2]/node1/node1"), hasChildren());

        Subgraph subgraph = graph.getSubgraphOfDepth(4).at("/node3");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("."), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("node2[2]"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1").getChildren(), hasChildren(segment("node1"),
                                                                                 segment("node2"),
                                                                                 segment("node3")));
        assertThat(subgraph.getNode("node2[2]/node1"), hasProperty("property1",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1"), hasProperty("property2",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1"), hasProperty("property3",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node1/node1"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node1"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node1"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node1/node2"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node2"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node2"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node1/node3"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node3"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node3"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        if (source.getCapabilities().supportsReferences()) {
            assertThat(subgraph.getNode("node2[2]/node2").getChildren(), hasChildren(segment("node1"),
                                                                                     segment("node2"),
                                                                                     segment("node3")));
        }
        assertThat(subgraph.getNode("node2[2]/node2"), hasProperty("property1",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2"), hasProperty("property2",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2"), hasProperty("property3",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node2/node1"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node1"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node1"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node2/node2"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node2"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node2"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node2/node3"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node3"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node3"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        if (source.getCapabilities().supportsReferences()) {
            assertThat(subgraph.getNode("node2[2]/node3").getChildren(), hasChildren(segment("node1"),
                                                                                     segment("node2"),
                                                                                     segment("node3")));
        }
        assertThat(subgraph.getNode("node2[2]/node3"), hasProperty("property1",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3"), hasProperty("property2",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3"), hasProperty("property3",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node3/node1"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node1"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node1"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node3/node2"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node2"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node2"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node3/node3"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node3"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node3"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
    }

    @Test
    public void shouldCopyChildrenBetweenWorkspacesWithNewUuids() throws Exception {
        String defaultWorkspaceName = graph.getCurrentWorkspaceName();
        String workspaceName = "copyChildrenSource";

        tryCreatingAWorkspaceNamed(workspaceName);

        graph.useWorkspace(workspaceName);
        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        Subgraph source = graph.getSubgraphOfDepth(3).at("/node1");

        graph.useWorkspace(defaultWorkspaceName);

        graph.create("/newUuids").and();
        graph.copy("/node1").fromWorkspace(workspaceName).to("/newUuids/node1");

        /*
         * Focus on testing node structure, since shouldCopyNodeWithChildren tests that properties get copied
         */
        Subgraph target = graph.getSubgraphOfDepth(3).at("/newUuids/node1");
        assertThat(target, is(notNullValue()));
        assertThat(target.getNode(".").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(uuidFor(target.getNode(".")), not(uuidFor(source.getNode("."))));
        assertThat(target.getNode("node1").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(uuidFor(target.getNode("node1")), not(uuidFor(source.getNode("node1"))));
        assertThat(target.getNode("node2").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(uuidFor(target.getNode("node2")), not(uuidFor(source.getNode("node2"))));
        assertThat(target.getNode("node3").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(uuidFor(target.getNode("node3")), not(uuidFor(source.getNode("node3"))));

    }

    @Test
    public void shouldNotCloneChildrenIfUuidConflictAndFailureBehavior() throws Exception {
        String defaultWorkspaceName = graph.getCurrentWorkspaceName();
        String workspaceName = "copyChildrenSource";

        tryCreatingAWorkspaceNamed(workspaceName);

        graph.useWorkspace(workspaceName);
        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        graph.useWorkspace(defaultWorkspaceName);

        graph.create("/newUuids").and();
        // Copy once to get the UUID into the default workspace
        // graph.copy("/node1/node1/node1").failingIfUuidsMatch().fromWorkspace(workspaceName).to("/newUuids/node1");
        graph.clone("/node1/node1/node1")
             .fromWorkspace(workspaceName)
             .as(name("node1"))
             .into("/newUuids")
             .failingIfAnyUuidsMatch();

        try {
            // Copy again to get the exception since the UUID is already in the default workspace
            // graph.copy("/node1/node1").failingIfUuidsMatch().fromWorkspace(workspaceName).to("/newUuids/shouldNotWork");
            graph.clone("/node1/node1/node1")
                 .fromWorkspace(workspaceName)
                 .as(name("shouldNotWork"))
                 .into("/newUuids")
                 .failingIfAnyUuidsMatch();
            fail("Should not be able to copy a node into a workspace if another node with the "
                 + "same UUID already exists in the workspace and UUID behavior is failingIfUuidsMatch");
        } catch (UuidAlreadyExistsException ex) {
            // Expected
        }

    }

    @Test
    public void shouldCloneChildrenAndRemoveExistingNodesWithSameUuidIfSpecified() throws Exception {
        String defaultWorkspaceName = graph.getCurrentWorkspaceName();
        String workspaceName = "copyChildrenSource";

        tryCreatingAWorkspaceNamed(workspaceName);

        graph.useWorkspace(workspaceName);
        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        Subgraph source = graph.getSubgraphOfDepth(3).at("/node1");

        graph.useWorkspace(defaultWorkspaceName);

        graph.create("/newUuids").and();
        // Copy once to get the UUID into the default workspace
        graph.clone("/node1")
             .fromWorkspace(workspaceName)
             .as(name("node1"))
             .into("/newUuids")
             .replacingExistingNodesWithSameUuids();

        // Make sure that the node wasn't moved by the clone
        graph.useWorkspace(workspaceName);
        graph.getNodeAt("/node1");
        graph.useWorkspace(defaultWorkspaceName);

        // Create a new child node that in the target workspace that has no corresponding node in the source workspace
        graph.create("/newUuids/node1/shouldBeRemoved");

        // Copy again to test the behavior now that the UUIDs are already in the default workspace
        // This should first remove /newUuids/node1, which of course will remove /newUuids/node1/shouldBeRemoved
        graph.clone("/node1")
             .fromWorkspace(workspaceName)
             .as(name("otherNode"))
             .into("/newUuids")
             .replacingExistingNodesWithSameUuids();

        /*
         * Focus on testing node structure, since shouldCopyNodeWithChildren tests that properties get copied
         */
        // /newUuids/node1 should have been removed when the new node was added with the same UUID
        assertThat(graph.getNodeAt("/newUuids").getChildren(), hasChildren(segment("otherNode")));

        try {
            graph.getNodeAt("/newUuids/node1/shouldBeRemoved");
            fail("/newUuids/node1/shouldBeRemoved should no longer exist after the copy-with-remove-conflicting-uuids operation");
        } catch (PathNotFoundException pnfe) {
            // Expected
        }

        Subgraph target = graph.getSubgraphOfDepth(3).at("/newUuids/otherNode");
        assertThat(target, is(notNullValue()));
        assertThat(target.getNode(".").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(uuidFor(target.getNode(".")), is(uuidFor(source.getNode("."))));
        assertThat(target.getNode("node1").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(uuidFor(target.getNode("node1")), is(uuidFor(source.getNode("node1"))));
        assertThat(target.getNode("node2").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(uuidFor(target.getNode("node2")), is(uuidFor(source.getNode("node2"))));
        assertThat(target.getNode("node3").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(uuidFor(target.getNode("node3")), is(uuidFor(source.getNode("node3"))));
    }

    @Test
    public void shouldCloneNodeIntoExactLocationIfSpecified() throws Exception {
        String defaultWorkspaceName = graph.getCurrentWorkspaceName();
        String workspaceName = "copyChildrenSource";

        tryCreatingAWorkspaceNamed(workspaceName);

        graph.useWorkspace(workspaceName);
        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        Subgraph source = graph.getSubgraphOfDepth(3).at("/node1");

        graph.useWorkspace(defaultWorkspaceName);

        graph.create("/segmentTestUuids").and();
        // Copy once to get the UUID into the default workspace
        graph.clone("/node1").fromWorkspace(workspaceName).as(name("node1")).into("/segmentTestUuids").failingIfAnyUuidsMatch();

        // Create a new child node that in the target workspace that has no corresponding node in the source workspace
        PropertyFactory propFactory = context.getPropertyFactory();
        graph.create("/segmentTestUuids/node1", propFactory.create(name("identifier"), "backup copy")).and();

        // Copy again to test the behavior now that the UUIDs are already in the default workspace
        // This should remove /segmentTestUuids/node1[1]
        graph.clone("/node1")
             .fromWorkspace(workspaceName)
             .as(segment("node1[1]"))
             .into("/segmentTestUuids")
             .replacingExistingNodesWithSameUuids();

        /*
         * Focus on testing node structure, since shouldCopyNodeWithChildren tests that properties get copied
         */
        // /segmentTestUuids/node1[1] should have been removed when the new node was added with the same UUID
        // Now "/segmentTestUuids/node1[1]" (which was "/segmentTestUuids/node1[2]") should have same UUID as the
        // node with "identifier=backup copy" property
        assertThat(graph.getNodeAt("/segmentTestUuids").getChildren(), hasChildren(segment("node1"), segment("node1[2]")));
        assertThat(graph.getNodeAt("/segmentTestUuids/node1[1]").getProperty("identifier"), is(nullValue()));
        assertThat(graph.getNodeAt("/segmentTestUuids/node1[2]").getProperty("identifier").getFirstValue().toString(),
                   is("backup copy"));

        Subgraph target = graph.getSubgraphOfDepth(3).at("/segmentTestUuids/node1[1]");
        assertThat(target, is(notNullValue()));
        assertThat(target.getNode(".").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(uuidFor(target.getNode(".")), is(uuidFor(source.getNode("."))));
        assertThat(target.getNode("node1").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(uuidFor(target.getNode("node1")), is(uuidFor(source.getNode("node1"))));
        assertThat(target.getNode("node2").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(uuidFor(target.getNode("node2")), is(uuidFor(source.getNode("node2"))));
        assertThat(target.getNode("node3").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(uuidFor(target.getNode("node3")), is(uuidFor(source.getNode("node3"))));
    }

    protected void tryCreatingAWorkspaceNamed( String workspaceName ) {
        try {
            graph.createWorkspace().named(workspaceName);
        } catch (Exception ex) {
            assumeNoException(ex);
        }
    }

    private UUID uuidFor( Node node ) {
        UUID uuid = null;
        if (node.getLocation().getUuid() != null) {
            uuid = node.getLocation().getUuid();
        }

        if (uuid == null && (node.getProperty(JcrLexicon.UUID) != null)) {
            uuid = (UUID)node.getProperty(JcrLexicon.UUID).getFirstValue();
        }

        return uuid;
    }

    protected void assertReference( String fromNodePath,
                                    String propertyName,
                                    String... toNodePath ) {
        Object[] values = graph.getProperty(propertyName).on(fromNodePath).getValuesAsArray();
        assertThat(values.length, is(toNodePath.length));
        for (int i = 0; i != values.length; ++i) {
            Object value = values[i];
            assertThat(value, is(instanceOf(Reference.class)));
            Reference ref = (Reference)value;
            assertThat(graph.resolve(ref).getLocation().getPath(), is(graph.getNodeAt(toNodePath[i]).getLocation().getPath()));
        }
    }

    @Test
    public void shouldMoveNodeBeforeSibling() {

        String initialPath = "";
        int depth = 1;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        // Check starting point ...
        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1"), segment("node2"), segment("node3")));

        // Move last node to front ...
        graph.move("/node3").before("/node1");

        assertThat(graph.getChildren().of("/"), hasChildren(segment("node3"), segment("node1"), segment("node2")));

        // Move last node to front ...
        graph.move("/node2").before("/node3");

        assertThat(graph.getChildren().of("/"), hasChildren(segment("node2"), segment("node3"), segment("node1")));

    }

    @Test
    public void shouldMoveNodeBeforeSelf() {

        String initialPath = "";
        int depth = 1;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        // Move node before itself (trivial change) ...
        graph.move("/node2").before("/node2");

        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
    }

    @Test
    public void shouldMoveNodes() {
        // Create the tree (at total of 40 nodes, plus the extra 6 added later)...
        // /
        // /node1
        // /node1/node1
        // /node1/node1/node1
        // /node1/node1/node2
        // /node1/node1/node3
        // /node1/node2
        // /node1/node2/node1
        // /node1/node2/node2
        // /node1/node2/node3
        // /node1/node3
        // /node1/node3/node1
        // /node1/node3/node2
        // /node1/node3/node3
        // /node2
        // /node2/node1
        // /node2/node1/node1
        // /node2/node1/node2
        // /node2/node1/node3
        // /node2/node2
        // /node2/node2/node1
        // /node2/node2/node2
        // /node2/node2/node3
        // /node2/node3
        // /node2/node3/node1
        // /node2/node3/node2
        // /node2/node3/node3
        // /node3
        // /node3/node1
        // /node3/node1/node1
        // /node3/node1/node2
        // /node3/node1/node3
        // /node3/node2
        // /node3/node2/node1
        // /node3/node2/node2
        // /node3/node2/node3
        // /node3/node3
        // /node3/node3/node1
        // /node3/node3/node2
        // /node3/node3/node3
        // /secondBranch1
        // /secondBranch1/secondBranch1
        // /secondBranch1/secondBranch2
        // /secondBranch2
        // /secondBranch2/secondBranch1
        // /secondBranch2/secondBranch2

        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        // Delete two branches ...
        graph.move("/node2").into("/node3");

        // Now assert the structure ...
        assertThat(graph.getChildren().of("/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3/node1"), hasChildren());

        assertThat(graph.getChildren().of("/node3"), hasChildren(segment("node1"),
                                                                 segment("node2"),
                                                                 segment("node3"),
                                                                 segment("node2[2]")));
        assertThat(graph.getChildren().of("/node3/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3/node1"), hasChildren());
        assertThat(graph.getChildren().of("/node3/node2[2]"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[2]/node1"), hasChildren(segment("node1"),
                                                                                segment("node2"),
                                                                                segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[2]/node2"), hasChildren(segment("node1"),
                                                                                segment("node2"),
                                                                                segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[2]/node3"), hasChildren(segment("node1"),
                                                                                segment("node2"),
                                                                                segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[2]/node1/node1"), hasChildren());

        Subgraph subgraph = graph.getSubgraphOfDepth(4).at("/node3");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("."), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("node2[2]"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1").getChildren(), hasChildren(segment("node1"),
                                                                                 segment("node2"),
                                                                                 segment("node3")));
        assertThat(subgraph.getNode("node2[2]/node1"), hasProperty("property1",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1"), hasProperty("property2",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1"), hasProperty("property3",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node1/node1"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node1"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node1"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node1/node2"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node2"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node2"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node1/node3"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node3"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1/node3"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2"), hasProperty("property1",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2"), hasProperty("property2",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2"), hasProperty("property3",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node2/node1"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node1"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node1"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node2/node2"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node2"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node2"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node2/node3"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node3"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node2/node3"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3"), hasProperty("property1",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3"), hasProperty("property2",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3"), hasProperty("property3",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node3/node1"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node1"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node1"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node3/node2"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node2"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node2"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[2]/node3/node3"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node3"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node3/node3"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
    }

    @Test
    public void shouldMoveAndRenameNodes() {
        // Create the tree (at total of 40 nodes, plus the extra 6 added later)...
        // /
        // /node1
        // /node1/node1
        // /node1/node1/node1
        // /node1/node1/node2
        // /node1/node1/node3
        // /node1/node2
        // /node1/node2/node1
        // /node1/node2/node2
        // /node1/node2/node3
        // /node1/node3
        // /node1/node3/node1
        // /node1/node3/node2
        // /node1/node3/node3
        // /node2
        // /node2/node1
        // /node2/node1/node1
        // /node2/node1/node2
        // /node2/node1/node3
        // /node2/node2
        // /node2/node2/node1
        // /node2/node2/node2
        // /node2/node2/node3
        // /node2/node3
        // /node2/node3/node1
        // /node2/node3/node2
        // /node2/node3/node3
        // /node3
        // /node3/node1
        // /node3/node1/node1
        // /node3/node1/node2
        // /node3/node1/node3
        // /node3/node2
        // /node3/node2/node1
        // /node3/node2/node2
        // /node3/node2/node3
        // /node3/node3
        // /node3/node3/node1
        // /node3/node3/node2
        // /node3/node3/node3
        // /secondBranch1
        // /secondBranch1/secondBranch1
        // /secondBranch1/secondBranch2
        // /secondBranch2
        // /secondBranch2/secondBranch1
        // /secondBranch2/secondBranch2

        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        // Delete two branches ...
        graph.move("/node2").as("node3").into("/node3");

        // Now assert the structure ...
        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1"), segment("node3")));
        assertThat(graph.getChildren().of("/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3/node1"), hasChildren());

        assertThat(graph.getChildren().of("/node3"), hasChildren(segment("node1"),
                                                                 segment("node2"),
                                                                 segment("node3"),
                                                                 segment("node3[2]")));
        assertThat(graph.getChildren().of("/node3/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3/node1"), hasChildren());
        assertThat(graph.getChildren().of("/node3/node3[2]"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3[2]/node1"), hasChildren(segment("node1"),
                                                                                segment("node2"),
                                                                                segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3[2]/node2"), hasChildren(segment("node1"),
                                                                                segment("node2"),
                                                                                segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3[2]/node3"), hasChildren(segment("node1"),
                                                                                segment("node2"),
                                                                                segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3[2]/node1/node1"), hasChildren());

        Subgraph subgraph = graph.getSubgraphOfDepth(4).at("/node3");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("."), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("node3[2]"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node1").getChildren(), hasChildren(segment("node1"),
                                                                                 segment("node2"),
                                                                                 segment("node3")));
        assertThat(subgraph.getNode("node3[2]/node1"), hasProperty("property1",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node1"), hasProperty("property2",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node1"), hasProperty("property3",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node1/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3[2]/node1/node1"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node1/node1"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node1/node1"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node1/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3[2]/node1/node2"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node1/node2"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node1/node2"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node1/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3[2]/node1/node3"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node1/node3"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node1/node3"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node2"), hasProperty("property1",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node2"), hasProperty("property2",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node2"), hasProperty("property3",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node2/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3[2]/node2/node1"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node2/node1"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node2/node1"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node2/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3[2]/node2/node2"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node2/node2"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node2/node2"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node2/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3[2]/node2/node3"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node2/node3"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node2/node3"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node3"), hasProperty("property1",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node3"), hasProperty("property2",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node3"), hasProperty("property3",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node3/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3[2]/node3/node1"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node3/node1"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node3/node1"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node3/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3[2]/node3/node2"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node3/node2"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node3/node2"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node3/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3[2]/node3/node3"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node3/node3"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3[2]/node3/node3"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
    }

    @Test
    public void shouldMoveAndRenameNodesToNameWithoutSameNameSibling() {
        // Create the tree (at total of 40 nodes, plus the extra 6 added later)...
        // /
        // /node1
        // /node1/node1
        // /node1/node1/node1
        // /node1/node1/node2
        // /node1/node1/node3
        // /node1/node2
        // /node1/node2/node1
        // /node1/node2/node2
        // /node1/node2/node3
        // /node1/node3
        // /node1/node3/node1
        // /node1/node3/node2
        // /node1/node3/node3
        // /node2
        // /node2/node1
        // /node2/node1/node1
        // /node2/node1/node2
        // /node2/node1/node3
        // /node2/node2
        // /node2/node2/node1
        // /node2/node2/node2
        // /node2/node2/node3
        // /node2/node3
        // /node2/node3/node1
        // /node2/node3/node2
        // /node2/node3/node3
        // /node3
        // /node3/node1
        // /node3/node1/node1
        // /node3/node1/node2
        // /node3/node1/node3
        // /node3/node2
        // /node3/node2/node1
        // /node3/node2/node2
        // /node3/node2/node3
        // /node3/node3
        // /node3/node3/node1
        // /node3/node3/node2
        // /node3/node3/node3
        // /secondBranch1
        // /secondBranch1/secondBranch1
        // /secondBranch1/secondBranch2
        // /secondBranch2
        // /secondBranch2/secondBranch1
        // /secondBranch2/secondBranch2

        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        // Delete two branches ...
        graph.move("/node2").as("nodeX").into("/node3");

        // Now assert the structure ...
        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1"), segment("node3")));
        assertThat(graph.getChildren().of("/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3/node1"), hasChildren());

        assertThat(graph.getChildren().of("/node3"), hasChildren(segment("node1"),
                                                                 segment("node2"),
                                                                 segment("node3"),
                                                                 segment("nodeX")));
        assertThat(graph.getChildren().of("/node3/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3/node1"), hasChildren());
        assertThat(graph.getChildren().of("/node3/nodeX"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/nodeX/node1"),
                   hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/nodeX/node2"),
                   hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/nodeX/node3"),
                   hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/nodeX/node1/node1"), hasChildren());

        Subgraph subgraph = graph.getSubgraphOfDepth(4).at("/node3");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("."), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("nodeX"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node1").getChildren(), hasChildren(segment("node1"),
                                                                              segment("node2"),
                                                                              segment("node3")));
        assertThat(subgraph.getNode("nodeX/node1"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node1"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node1"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node1/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("nodeX/node1/node1"), hasProperty("property1",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node1/node1"), hasProperty("property2",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node1/node1"), hasProperty("property3",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node1/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("nodeX/node1/node2"), hasProperty("property1",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node1/node2"), hasProperty("property2",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node1/node2"), hasProperty("property3",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node1/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("nodeX/node1/node3"), hasProperty("property1",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node1/node3"), hasProperty("property2",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node1/node3"), hasProperty("property3",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node2"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node2"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node2"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node2/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("nodeX/node2/node1"), hasProperty("property1",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node2/node1"), hasProperty("property2",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node2/node1"), hasProperty("property3",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node2/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("nodeX/node2/node2"), hasProperty("property1",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node2/node2"), hasProperty("property2",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node2/node2"), hasProperty("property3",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node2/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("nodeX/node2/node3"), hasProperty("property1",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node2/node3"), hasProperty("property2",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node2/node3"), hasProperty("property3",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node3"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node3"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node3"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node3/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("nodeX/node3/node1"), hasProperty("property1",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node3/node1"), hasProperty("property2",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node3/node1"), hasProperty("property3",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node3/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("nodeX/node3/node2"), hasProperty("property1",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node3/node2"), hasProperty("property2",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node3/node2"), hasProperty("property3",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node3/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("nodeX/node3/node3"), hasProperty("property1",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node3/node3"), hasProperty("property2",
                                                                      "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("nodeX/node3/node3"), hasProperty("property3",
                                                                      "The quick brown fox jumped over the moon. What? "));
    }

    @Test
    public void shouldMoveAndRenameNodesToNameWithSameNameSibling() {
        // Create the tree (at total of 40 nodes, plus the extra 6 added later)...
        // /
        // /node1
        // /node1/node1
        // /node1/node1/node1
        // /node1/node1/node2
        // /node1/node1/node3
        // /node1/node2
        // /node1/node2/node1
        // /node1/node2/node2
        // /node1/node2/node3
        // /node1/node3
        // /node1/node3/node1
        // /node1/node3/node2
        // /node1/node3/node3
        // /node2
        // /node2/node1
        // /node2/node1/node1
        // /node2/node1/node2
        // /node2/node1/node3
        // /node2/node2
        // /node2/node2/node1
        // /node2/node2/node2
        // /node2/node2/node3
        // /node2/node3
        // /node2/node3/node1
        // /node2/node3/node2
        // /node2/node3/node3
        // /node3
        // /node3/node1
        // /node3/node1/node1
        // /node3/node1/node2
        // /node3/node1/node3
        // /node3/node2
        // /node3/node2/node1
        // /node3/node2/node2
        // /node3/node2/node3
        // /node3/node3
        // /node3/node3/node1
        // /node3/node3/node2
        // /node3/node3/node3
        // /secondBranch1
        // /secondBranch1/secondBranch1
        // /secondBranch1/secondBranch2
        // /secondBranch2
        // /secondBranch2/secondBranch1
        // /secondBranch2/secondBranch2

        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        // Delete two branches ...
        graph.move("/node2").before("/node3/node2");

        // Now assert the structure ...
        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1"), segment("node3")));
        assertThat(graph.getChildren().of("/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node1"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node2"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node1/node3/node1"), hasChildren());

        assertThat(graph.getChildren().of("/node3"), hasChildren(segment("node1"),
                                                                 segment("node2[1]"),
                                                                 segment("node2[2]"),
                                                                 segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[1]"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node3/node1"), hasChildren());
        assertThat(graph.getChildren().of("/node3/node2[1]"), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[1]/node1"), hasChildren(segment("node1"),
                                                                                segment("node2"),
                                                                                segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[1]/node2"), hasChildren(segment("node1"),
                                                                                segment("node2"),
                                                                                segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[1]/node3"), hasChildren(segment("node1"),
                                                                                segment("node2"),
                                                                                segment("node3")));
        assertThat(graph.getChildren().of("/node3/node2[1]/node1/node1"), hasChildren());

        Subgraph subgraph = graph.getSubgraphOfDepth(4).at("/node3");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("."), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("node2[2]"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]").getChildren(), hasChildren(segment("node1"), segment("node2"), segment("node3")));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node1").getChildren(), hasChildren(segment("node1"),
                                                                                 segment("node2"),
                                                                                 segment("node3")));
        assertThat(subgraph.getNode("node2[1]/node1"), hasProperty("property1",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node1"), hasProperty("property2",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node1"), hasProperty("property3",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node1/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[1]/node1/node1"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node1/node1"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node1/node1"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node1/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[1]/node1/node2"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node1/node2"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node1/node2"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node1/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[1]/node1/node3"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node1/node3"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node1/node3"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node2"), hasProperty("property1",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node2"), hasProperty("property2",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node2"), hasProperty("property3",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node2/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[1]/node2/node1"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node2/node1"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node2/node1"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node2/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[1]/node2/node2"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node2/node2"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node2/node2"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node2/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[1]/node2/node3"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node2/node3"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node2/node3"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node3"), hasProperty("property1",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node3"), hasProperty("property2",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node3"), hasProperty("property3",
                                                                   "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node3/node1").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[1]/node3/node1"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node3/node1"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node3/node1"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node3/node2").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[1]/node3/node2"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node3/node2"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node3/node2"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node3/node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node2[1]/node3/node3"), hasProperty("property1",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node3/node3"), hasProperty("property2",
                                                                         "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]/node3/node3"), hasProperty("property3",
                                                                         "The quick brown fox jumped over the moon. What? "));
    }

    @Test
    public void shouldAddValuesToExistingProperty() {

        String initialPath = "";
        int depth = 1;
        int numChildrenPerNode = 1;
        int numPropertiesPerNode = 1;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1")));
        Subgraph subgraph = graph.getSubgraphOfDepth(2).at("/");
        assertThat(subgraph, is(notNullValue()));

        assertThat(subgraph.getNode("node1"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));

        graph.addValue("foo").andValue("bar").to("property1").on("node1");

        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1")));
        subgraph = graph.getSubgraphOfDepth(2).at("/");
        assertThat(subgraph, is(notNullValue()));

        assertThat(subgraph.getNode("node1"), hasProperty("property1",
                                                          "The quick brown fox jumped over the moon. What? ",
                                                          "foo",
                                                          "bar"));
    }

    @Test
    public void shouldAddValuesToNonExistantProperty() {

        String initialPath = "";
        int depth = 1;
        int numChildrenPerNode = 1;
        int numPropertiesPerNode = 1;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1")));

        graph.addValue("foo").andValue("bar").to("newProperty").on("node1");

        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1")));
        Subgraph subgraph = graph.getSubgraphOfDepth(2).at("/");
        assertThat(subgraph, is(notNullValue()));

        assertThat(subgraph.getNode("node1"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node1"), hasProperty("newProperty", "foo", "bar"));

    }

    @Test
    public void shouldRemoveValuesFromExistingProperty() {

        String initialPath = "";
        int depth = 1;
        int numChildrenPerNode = 1;
        int numPropertiesPerNode = 1;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1")));

        graph.removeValue("The quick brown fox jumped over the moon. What? ").andValue("bar").from("property1").on("node1");

        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1")));
        Subgraph subgraph = graph.getSubgraphOfDepth(2).at("/");
        assertThat(subgraph, is(notNullValue()));

        assertThat(subgraph.getNode("node1"), hasProperty("property1"));
    }

    @Test
    public void shouldNotRemoveValuesFromNonExistantProperty() {

        String initialPath = "";
        int depth = 1;
        int numChildrenPerNode = 1;
        int numPropertiesPerNode = 1;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, output, null);

        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1")));

        graph.removeValue("The quick brown fox jumped over the moon. What? ").from("noSuchProperty").on("node1");

        assertThat(graph.getChildren().of("/"), hasChildren(segment("node1")));
        Subgraph subgraph = graph.getSubgraphOfDepth(2).at("/");
        assertThat(subgraph, is(notNullValue()));

        assertThat(subgraph.getNode("node1"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
    }

    @Test
    public void shouldLockNode() {
        // fail("Need to add test body here");
    }

    @Test
    public void shouldNotAllowMultipleConcurrentLocksOnSameNode() {
        // fail("Need to add test body here");
    }

    @Test
    public void shouldUnlockNode() {
        // fail("Need to add test body here");
    }

    /**
     * This method attempts to create a small subgraph and then delete some of the nodes in that subgraph and then recreate some
     * of the just-deleted nodes, all within the same batch operation.
     */
    @FixFor( "MODE-788" )
    @Test
    public void shouldCreateAndDeleteAndRecreateInSameOperation() {
        graph.batch()
             .create("/a")
             .with("propB", "valueB")
             .and("propC", "valueC")
             .and()
             .create("/a/b")
             .with("propA", "value a/b")
             .and()
             .create("/a/b/c")
             .with("propA", "value a/b/c")
             .and()
             .delete("/a/b")
             .and()
             .create("/a/b")
             .with("propA", "value a/b2")
             .and()
             .create("/a/b/c")
             .with("propA", "value a/b2/c")
             .and()
             .execute();

        // Now look up the root node ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(ModeShapeLexicon.UUID, getRootNodeUuid()));
        assertThat(root.getChildren(), hasChild(segment("a")));

        // Now look up node A ...
        Node nodeA = graph.getNodeAt("/a");
        assertThat(nodeA, is(notNullValue()));
        assertThat(nodeA, hasProperty("propB", "valueB"));
        assertThat(nodeA, hasProperty("propC", "valueC"));
        assertThat(nodeA.getChildren(), hasChild(segment("b")));

        // Now look up node B ...
        Node nodeB = graph.getNodeAt("/a/b");
        assertThat(nodeB, is(notNullValue()));
        assertThat(nodeB, hasProperty("propA", "value a/b2"));
        assertThat(nodeB.getChildren(), hasChild(segment("c")));

        // Now look up node C ...
        Node nodeC = graph.getNodeAt("/a/b/c");
        assertThat(nodeC, is(notNullValue()));
        assertThat(nodeC, hasProperty("propA", "value a/b2/c"));
        assertThat(nodeC.getChildren(), isEmpty());
    }

    /**
     * This method attempts to create a small subgraph and then delete some of the nodes in that subgraph, all within the same
     * batch operation.
     */
    @FixFor( "MODE-788" )
    @Test
    public void shouldCreateSubgraphAndDeletePartOfThatSubgraphInSameOperation() {
        graph.batch()
             .create("/a")
             .with("propB", "valueB")
             .and("propC", "valueC")
             .and()
             .create("/a/b")
             .with("propA", "value a/b")
             .and()
             .create("/a/b/c")
             .with("propA", "valueA a/b/c")
             .and()
             .delete("/a/b")
             .and()
             .execute();

        // Now look up the root node ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(ModeShapeLexicon.UUID, getRootNodeUuid()));
        assertThat(root.getChildren(), hasChild(segment("a")));

        // Now look up node A ...
        Node nodeA = graph.getNodeAt("/a");
        assertThat(nodeA, is(notNullValue()));
        assertThat(nodeA, hasProperty("propB", "valueB"));
        assertThat(nodeA, hasProperty("propC", "valueC"));
        assertThat(nodeA.getChildren(), isEmpty());
    }

    /**
     * This method attempts to create a small subgraph and then delete all of the nodes in that subgraph, all within the same
     * batch operation.
     */
    @FixFor( "MODE-788" )
    @Test
    public void shouldCreateAndDeleteSubgraphInSameOperation() {
        graph.batch()
             .create("/a")
             .with("propB", "valueB")
             .and("propC", "valueC")
             .and()
             .create("/a/b")
             .with("propA", "value a/b")
             .and()
             .create("/a/b/c")
             .with("propA", "valueA a/b/c")
             .and()
             .delete("/a")
             .and()
             .execute();

        // Now look up the root node ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(ModeShapeLexicon.UUID, getRootNodeUuid()));
        assertThat(root.getChildren(), isEmpty());
    }

    /**
     * This method attempts to create a very small subgraph and then delete all of the nodes in that subgraph, all within the same
     * batch operation.
     */
    @FixFor( "MODE-788" )
    @Test
    public void shouldCreateAndDeleteTwoNodeSubgraphInSameOperation() {
        // Look for the root before we do anyting ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(ModeShapeLexicon.UUID, getRootNodeUuid()));
        assertThat(root.getChildren(), isEmpty());

        graph.batch().create("/a").and().create("/a/b").and().delete("/a").and().execute();

        // Now look up the root node ...
        root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(ModeShapeLexicon.UUID, getRootNodeUuid()));
        assertThat(root.getChildren(), isEmpty());
    }

    /**
     * This method attempts to create a small subgraph and then delete all of the nodes in that subgraph, all within the same
     * batch operation.
     */
    @FixFor( "MODE-788" )
    @Test
    public void shouldCreateAndDeleteNodeInSameOperation() {
        graph.batch().create("/a").with("propB", "valueB").and("propC", "valueC").and().delete("/a").and().execute();
    }

}
