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
package org.jboss.dna.graph.connector.test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.jboss.dna.graph.IsNodeWithChildren.hasChild;
import static org.jboss.dna.graph.IsNodeWithChildren.hasChildren;
import static org.jboss.dna.graph.IsNodeWithChildren.isEmpty;
import static org.jboss.dna.graph.IsNodeWithProperty.hasProperty;
import static org.junit.Assert.assertThat;
import org.jboss.dna.common.statistic.Stopwatch;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.property.Reference;
import org.junit.Before;
import org.junit.Test;

/**
 * A class that provides standard writing verification tests for connectors that are able to store any content (in any structure).
 * This class is designed to be extended for each connector, and in each subclass the {@link #setUpSource()} method is defined to
 * provide a valid {@link RepositorySource} for the connector to be tested.
 * <p>
 * Since these tests do modify repository content, the repository is set up for each test, given each test a pristine repository
 * (as {@link #initializeContent(Graph) initialized} by the concrete test case class).
 * </p>
 * 
 * @author Randall Hauch
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
        assertThat(root, hasProperty(DnaLexicon.UUID, getRootNodeUuid()));
        assertThat(root, hasProperty("propA", "valueA"));
    }

    @Test
    public void shouldAddChildUnderRootNode() {
        graph.batch().create("/a").with("propB", "valueB").and("propC", "valueC").execute();
        // Now look up the root node ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(DnaLexicon.UUID, getRootNodeUuid()));
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
        graph.batch().set("propA").to("valueA").on("/").and().create("/a").with("propB", "valueB").and("propC", "valueC").and().create("/b").with("propD",
                                                                                                                                                  "valueD").and("propE",
                                                                                                                                                                "valueE").execute();
        // Now look up the root node ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(DnaLexicon.UUID, getRootNodeUuid()));
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
        assertThat(subgraph.getNode("."), hasProperty(DnaLexicon.UUID, getRootNodeUuid()));
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
        create.execute();
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
        create.execute();

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
        create.execute();

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
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, description);
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
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, description);
    }

    @Test
    public void shouldCreateFlatAndWideTreeUsingOneBatch() {
        String initialPath = "";
        int depth = 1;
        int numChildrenPerNode = 300;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, null);
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
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, description);
    }

    @Test
    public void shouldCreateTreeWith10ChildrenAnd2LevelsDeepUsingIndividualRequests() {
        String initialPath = "";
        int depth = 2;
        int numChildrenPerNode = 10;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = false;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, null);
    }

    @Test
    public void shouldCreateTreeWith10ChildrenAnd2LevelsDeepUsingOneBatch() {
        String initialPath = "";
        int depth = 2;
        int numChildrenPerNode = 10;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, null);
    }

    @Test
    public void shouldCreateTreeWith10ChildrenAnd3LevelsDeepUsingOneBatch() {
        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 10;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, null);
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
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, null);

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
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, null);

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
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, null);

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

        // Copy a branches ...
        graph.copy("/node2").into("/node3");

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
        assertThat(subgraph.getNode("node2[2]/node2").getChildren(), hasChildren(segment("node1"),
                                                                                 segment("node2"),
                                                                                 segment("node3")));
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
        assertThat(subgraph.getNode("node2[2]/node3").getChildren(), hasChildren(segment("node1"),
                                                                                 segment("node2"),
                                                                                 segment("node3")));
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

    protected void assertReference( String fromNodePath,
                                    String propertyName,
                                    String... toNodePath ) {
        Object[] values = graph.getProperty(propertyName).on(fromNodePath).getValuesAsArray();
        assertThat(values.length, is(toNodePath.length));
        for (int i = 0; i != values.length; ++i) {
            Object value = values[i];
            assertThat(value, is(instanceOf(Reference.class)));
            Reference ref = (Reference)value;
            assertThat(graph.resolve(ref), is(graph.getNodeAt(toNodePath[i])));
        }
    }

}
