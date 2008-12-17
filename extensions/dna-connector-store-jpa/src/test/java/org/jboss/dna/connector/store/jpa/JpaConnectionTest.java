/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.store.jpa;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.jboss.dna.graph.IsNodeWithChildren.hasChild;
import static org.jboss.dna.graph.IsNodeWithChildren.hasChildren;
import static org.jboss.dna.graph.IsNodeWithChildren.isEmpty;
import static org.jboss.dna.graph.IsNodeWithProperty.hasProperty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.jboss.dna.common.stats.Stopwatch;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.connector.store.jpa.models.basic.BasicModel;
import org.jboss.dna.graph.BasicExecutionContext;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the JpaConnection class using a {@link #getModel() model} for the entire set of tests. To run these same methods using a
 * different {@link Model}, subclass this class and override the {@link #getModel()} method to return the desired Model instance.
 * 
 * @author Randall Hauch
 */
public class JpaConnectionTest {

    private ExecutionContext context;
    private JpaConnection connection;
    private EntityManagerFactory factory;
    private EntityManager manager;
    private Model model;
    private CachePolicy cachePolicy;
    private UUID rootNodeUuid;
    private long largeValueSize;
    private boolean compressData;
    private boolean enforceReferentialIntegrity;
    private Graph graph;
    private String[] validLargeValues;
    private int numPropsOnEach;

    @Before
    public void beforeEach() throws Exception {
        context = new BasicExecutionContext();
        model = getModel();
        rootNodeUuid = UUID.randomUUID();
        largeValueSize = 2 ^ 10; // 1 kilobyte
        compressData = true;
        enforceReferentialIntegrity = true;
        numPropsOnEach = 0;

        // Load in the large value ...
        validLargeValues = new String[] {IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum1.txt")),
            IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum2.txt")),
            IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum3.txt"))};

        // Connect to the database ...
        Ejb3Configuration configurator = new Ejb3Configuration();
        model.configure(configurator);
        configureDatabaseProperties(configurator);
        configurator.setProperty("hibernate.show_sql", "false");
        configurator.setProperty("hibernate.format_sql", "true");
        configurator.setProperty("hibernate.use_sql_comments", "true");
        configurator.setProperty("hibernate.hbm2ddl.auto", "create");
        factory = configurator.buildEntityManagerFactory();
        manager = factory.createEntityManager();

        // Create the connection ...
        cachePolicy = mock(CachePolicy.class);
        connection = new JpaConnection("source", cachePolicy, manager, model, rootNodeUuid, largeValueSize, compressData,
                                       enforceReferentialIntegrity);

        // And create the graph ...
        graph = Graph.create(connection, context);
    }

    protected void configureDatabaseProperties( Ejb3Configuration configurator ) {
        configurator.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        configurator.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        configurator.setProperty("hibernate.connection.username", "sa");
        configurator.setProperty("hibernate.connection.password", "");
        configurator.setProperty("hibernate.connection.url", "jdbc:hsqldb:.");
    }

    @After
    public void afterEach() throws Exception {
        try {
            if (connection != null) connection.close();
        } finally {
            try {
                if (manager != null) manager.close();
            } finally {
                manager = null;
                if (factory != null) {
                    try {
                        factory.close();
                    } finally {
                        factory = null;
                    }
                }
            }
        }
    }

    @Test
    public void shouldFindLargeValueContentFromFile() {
        for (int i = 0; i != validLargeValues.length; ++i) {
            assertThat(validLargeValues[i].startsWith((i + 1) + ". Lorem ipsum dolor sit amet"), is(true));
        }
    }

    /**
     * Override this method in subclasses to create test cases that test other models.
     * 
     * @return the model that should be used in the test
     */
    protected Model getModel() {
        return new BasicModel();
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Property property( String name,
                                 Object... values ) {
        Name propName = name(name);
        return context.getPropertyFactory().create(propName, values);
    }

    protected Path.Segment child( String name ) {
        return context.getValueFactories().getPathFactory().createSegment(name);
    }

    @Test
    public void shouldAlwaysReadRootNodeByPath() {
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root.getProperty(DnaLexicon.UUID).getFirstValue(), is((Object)rootNodeUuid));
        assertThat(root.getLocation().getPath(), is(path("/")));
        assertThat(root.getLocation().getUuid(), is(rootNodeUuid));
    }

    @Test
    public void shouldAlwaysReadRootNodeByUuid() {
        Location location = new Location(rootNodeUuid);
        Node root = graph.getNodeAt(location);
        assertThat(root, is(notNullValue()));
        assertThat(root.getProperty(DnaLexicon.UUID).getFirstValue(), is((Object)rootNodeUuid));
        assertThat(root.getLocation().getPath(), is(path("/")));
        assertThat(root.getLocation().getUuid(), is(rootNodeUuid));
    }

    @Test
    public void shouldSetPropertyOnRootNode() {
        graph.set("propA").to("valueA").on("/");
        // Now look up the node ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(DnaLexicon.UUID, rootNodeUuid));
        assertThat(root, hasProperty("propA", "valueA"));
    }

    @Test
    public void shouldAddChildUnderRootNode() {
        graph.batch().create("/a").with("propB", "valueB").and("propC", "valueC").execute();
        // Now look up the root node ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(DnaLexicon.UUID, rootNodeUuid));
        assertThat(root.getChildren(), hasChild(child("a")));

        // Now look up node A ...
        Node nodeA = graph.getNodeAt("/a");
        assertThat(nodeA, is(notNullValue()));
        assertThat(nodeA, hasProperty("propB", "valueB"));
        assertThat(nodeA, hasProperty("propC", "valueC"));
        assertThat(nodeA.getChildren(), isEmpty());
    }

    @Test
    public void shouldAddChildrenOnRootNode() {
        graph.batch().set("propA").to("valueA").on("/").and().create("/a").with("propB", "valueB").and("propC", "valueC").and().create("/b").with("propD",
                                                                                                                                                  "valueD").and("propE",
                                                                                                                                                                "valueE").execute();
        // Now look up the root node ...
        Node root = graph.getNodeAt("/");
        assertThat(root, is(notNullValue()));
        assertThat(root, hasProperty(DnaLexicon.UUID, rootNodeUuid));
        assertThat(root, hasProperty("propA", "valueA"));
        assertThat(root.getChildren(), hasChildren(child("a"), child("b")));

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
        assertThat(subgraph.getNode("."), hasProperty(DnaLexicon.UUID, rootNodeUuid));
        assertThat(subgraph.getNode("."), hasProperty("propA", "valueA"));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(child("a"), child("b")));
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
    public void shouldGetOnePropertyOnNode() {
        Graph.Create<Graph.Batch> create = graph.batch().create("/a");
        for (int i = 0; i != 100; ++i) {
            create = create.with("property" + i, "value" + i);
        }
        create.execute();
        // Now get a single property ...
        Property p = graph.getProperty("property75").on("/a");
        assertThat(p, is(notNullValue()));
        assertThat(p.size(), is(1));
        assertThat(p.isSingle(), is(true));
        assertThat(p.getFirstValue().toString(), is("value75"));
    }

    @Test
    public void shouldCalculateNumberOfNodesInTreeCorrectly() {
        assertThat(numberNodesInTree(2, 2), is(7));
        assertThat(numberNodesInTree(2, 3), is(15));
        assertThat(numberNodesInTree(2, 4), is(31));
        assertThat(numberNodesInTree(3, 2), is(13));
        assertThat(numberNodesInTree(3, 3), is(40));
        assertThat(numberNodesInTree(3, 4), is(121));
        assertThat(numberNodesInTree(3, 5), is(364));
        assertThat(numberNodesInTree(3, 6), is(1093));
        assertThat(numberNodesInTree(3, 7), is(3280));
        assertThat(numberNodesInTree(3, 8), is(9841));
        assertThat(numberNodesInTree(3, 9), is(29524));
        assertThat(numberNodesInTree(4, 2), is(21));
        assertThat(numberNodesInTree(4, 3), is(85));
        assertThat(numberNodesInTree(4, 4), is(341));
        assertThat(numberNodesInTree(4, 5), is(1365));
        assertThat(numberNodesInTree(4, 6), is(5461));
        assertThat(numberNodesInTree(4, 7), is(21845));
        assertThat(numberNodesInTree(5, 3), is(156));
        assertThat(numberNodesInTree(5, 4), is(781));
        assertThat(numberNodesInTree(5, 5), is(3906));
        assertThat(numberNodesInTree(7, 3), is(400));
        assertThat(numberNodesInTree(7, 4), is(2801));
        assertThat(numberNodesInTree(7, 5), is(19608));
        assertThat(numberNodesInTree(8, 3), is(585));
        assertThat(numberNodesInTree(8, 4), is(4681));
        assertThat(numberNodesInTree(8, 5), is(37449));
        assertThat(numberNodesInTree(8, 6), is(299593));
        assertThat(numberNodesInTree(8, 7), is(2396745));
        assertThat(numberNodesInTree(10, 2), is(111));
        assertThat(numberNodesInTree(10, 3), is(1111));
        assertThat(numberNodesInTree(10, 4), is(11111));
        assertThat(numberNodesInTree(100, 1), is(101));
        assertThat(numberNodesInTree(200, 1), is(201));
        assertThat(numberNodesInTree(200, 2), is(40201));
        assertThat(numberNodesInTree(200, 3), is(8040201));
        assertThat(numberNodesInTree(1000, 1), is(1001));
        assertThat(numberNodesInTree(3000, 1), is(3001));
    }

    @Test
    public void shouldCreateDeepBranch() {
        createTree("", 1, 50, numPropsOnEach, "deep and narrow tree, 1x50", false, false);
    }

    @Test
    public void shouldCreateDeepBranchUsingOneBatch() {
        createTree("", 1, 50, numPropsOnEach, "deep and narrow tree, 1x50", true, false);
    }

    // @Test
    // public void shouldCreateBinaryTree() {
    // createTree("", 2, 8, "binary tree, 2x8", false,false);
    // }

    @Test
    public void shouldCreate10x2Tree() {
        createTree("", 10, 2, numPropsOnEach, null, false, false);
    }

    @Test
    public void shouldCreate10x2TreeUsingOneBatch() {
        createTree("", 10, 2, numPropsOnEach, null, true, false);
    }

    @Test
    public void shouldCreate10x2TreeWith10PropertiesOnEachNodeUsingOneBatch() {
        numPropsOnEach = 10;
        createTree("", 10, 2, numPropsOnEach, null, true, false);
    }

    // @Test
    // public void shouldCreate10x3DecimalTree() {
    // createTree("", 10, 3, numPropsOnEach, null, false, true);
    // }
    //
    // @Test
    // public void shouldCreate10x3DecimalTreeUsingOneBatch() {
    // createTree("", 10, 3, numPropsOnEach, null, true, false);
    // }
    //
    // @Test
    // public void shouldCreate10x3DecimalTreeWith10PropertiesOnEachNodeCompressedUsingOneBatch() {
    // // compressData = true; the default
    // connection = new JpaConnection("source", cachePolicy, manager, model, rootNodeUuid, largeValueSize, compressData);
    // graph = Graph.create(connection, context);
    //
    // numPropsOnEach = 10;
    // createTree("", 10, 3, numPropsOnEach, null, true, false);
    // }
    //
    // @Test
    // public void shouldCreate10x3DecimalTreeWith10PropertiesOnEachNodeUsingOneBatch() {
    // compressData = false;
    // connection = new JpaConnection("source", cachePolicy, manager, model, rootNodeUuid, largeValueSize, compressData);
    // graph = Graph.create(connection, context);
    //
    // numPropsOnEach = 10;
    // createTree("", 10, 3, numPropsOnEach, null, true, false);
    // }

    @Test
    public void shouldReadNodes() {
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

        numPropsOnEach = 3;
        createTree("", 3, 3, numPropsOnEach, null, true, false);
        assertThat(graph.getChildren().of("/node1"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node1/node1"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node1/node2"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node1/node3"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node1/node3/node1"), isEmpty());

        assertThat(graph.getChildren().of("/node2"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node2/node1"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node2/node2"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node2/node3"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node2/node3/node1"), isEmpty());

        assertThat(graph.getChildren().of("/node3"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node1"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node2"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node3"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node3/node1"), isEmpty());

        Subgraph subgraph = graph.getSubgraphOfDepth(2).at("/node3/node2");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(child("node1"), child("node2"), child("node3")));
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
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(child("node1"), child("node2"), child("node3")));
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

        numPropsOnEach = 3;
        createTree("", 3, 3, numPropsOnEach, null, true, false);

        // Delete two branches ...
        graph.delete("/node2/node2").and().delete("/node3/node1");

        assertThat(graph.getChildren().of("/node1"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node1/node1"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node1/node2"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node1/node3"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node1/node3/node1"), hasChildren());

        assertThat(graph.getChildren().of("/node2"), hasChildren(child("node1"), child("node3")));
        assertThat(graph.getChildren().of("/node2/node1"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node2/node3"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node2/node3/node1"), hasChildren());

        assertThat(graph.getChildren().of("/node3"), hasChildren(child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node2"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node3"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node3/node1"), hasChildren());

        Subgraph subgraph = graph.getSubgraphOfDepth(3).at("/");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(child("node1"), child("node3")));
        assertThat(subgraph.getNode("node1").getChildren(), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(subgraph.getNode("node1"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node1"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node1"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2").getChildren(), hasChildren(child("node1"), child("node3")));
        assertThat(subgraph.getNode("node2"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3").getChildren(), hasChildren(child("node2"), child("node3")));
        assertThat(subgraph.getNode("node3"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));

        subgraph = graph.getSubgraphOfDepth(2).at("/node3");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(child("node2"), child("node3")));
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

        numPropsOnEach = 3;
        createTree("", 3, 3, numPropsOnEach, null, true, false);

        // Copy a branches ...
        graph.copy("/node2").into("/node3");

        assertThat(graph.getChildren().of("/node1"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node1/node1"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node1/node2"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node1/node3"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node1/node3/node1"), hasChildren());

        // The original of the copy should still exist ...
        assertThat(graph.getChildren().of("/node2"), hasChildren(child("node1"), child("node3")));
        assertThat(graph.getChildren().of("/node2/node1"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node2/node3"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node2/node3/node1"), hasChildren());

        assertThat(graph.getChildren().of("/node3"), hasChildren(child("node2[1]"), child("node3"), child("node2[2]")));
        assertThat(graph.getChildren().of("/node3/node2[1]"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node3"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node3/node1"), hasChildren());
        assertThat(graph.getChildren().of("/node3/node2[2]"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node2[2]/node1"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node2[2]/node2"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node2[2]/node3"), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(graph.getChildren().of("/node3/node2[2]/node1/node1"), hasChildren());

        Subgraph subgraph = graph.getSubgraphOfDepth(4).at("/node3");
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getNode(".").getChildren(), hasChildren(child("node2"), child("node3")));
        assertThat(subgraph.getNode("."), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("."), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]").getChildren(), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[1]"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3").getChildren(), isEmpty());
        assertThat(subgraph.getNode("node3"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node3"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]").getChildren(), hasChildren(child("node1"), child("node2"), child("node3")));
        assertThat(subgraph.getNode("node2[2]"), hasProperty("property1", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]"), hasProperty("property2", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]"), hasProperty("property3", "The quick brown fox jumped over the moon. What? "));
        assertThat(subgraph.getNode("node2[2]/node1").getChildren(), hasChildren(child("node1"), child("node2"), child("node3")));
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
        assertThat(subgraph.getNode("node2[2]/node2").getChildren(), hasChildren(child("node1"), child("node2"), child("node3")));
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
        assertThat(subgraph.getNode("node2[2]/node3").getChildren(), hasChildren(child("node1"), child("node2"), child("node3")));
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
    public void shouldReadRangeOfChildren() {
        // Create a shallow tree with many children under one node ...
        // /
        // /node1
        // /node1/node1
        // /node1/node2
        // ...
        // /node1/node10
        // /node1/secondBranch1
        // ...

        graph.batch().create("/node1").with("prop1", "value1").and("prop2", "value2").execute();
        numPropsOnEach = 3;
        createTree("/node1", 10, 2, numPropsOnEach, null, true, false);

        // Verify that the children were created ...
        List<Location> allChildren = graph.getChildren().of("/node1");
        assertThat(allChildren, hasChildren(child("node1"),
                                            child("node2"),
                                            child("node3"),
                                            child("node4"),
                                            child("node5"),
                                            child("node6"),
                                            child("node7"),
                                            child("node8"),
                                            child("node9"),
                                            child("node10")));

        // Now test reading children in various ranges ...
        List<Location> children = graph.getChildren().inBlockOf(4).startingAt(4).under("/node1");
        assertThat(children, is(notNullValue()));
        assertThat(children, hasChildren(child("node5"), child("node6"), child("node7"), child("node8")));

        children = graph.getChildren().inBlockOf(3).startingAt(4).under("/node1");
        assertThat(children, is(notNullValue()));
        assertThat(children, hasChildren(child("node5"), child("node6"), child("node7")));

        children = graph.getChildren().inBlockOf(10).startingAt(7).under("/node1");
        assertThat(children, is(notNullValue()));
        assertThat(children, hasChildren(child("node8"), child("node9"), child("node10")));
    }

    @Test
    public void shouldReadNextBlockOfChildren() {
        // Create a shallow tree with many children under one node ...
        // /
        // /node1
        // /node1/node1
        // /node1/node2
        // ...
        // /node1/node10
        // /node1/secondBranch1
        // ...

        graph.batch().create("/node1").with("prop1", "value1").and("prop2", "value2").execute();
        numPropsOnEach = 3;
        createTree("/node1", 10, 2, numPropsOnEach, null, true, false);

        // Verify that the children were created ...
        List<Location> allChildren = graph.getChildren().of("/node1");
        assertThat(allChildren, hasChildren(child("node1"),
                                            child("node2"),
                                            child("node3"),
                                            child("node4"),
                                            child("node5"),
                                            child("node6"),
                                            child("node7"),
                                            child("node8"),
                                            child("node9"),
                                            child("node10")));

        // Now test reading children in various ranges ...
        Location node4 = allChildren.get(3);
        List<Location> children = graph.getChildren().inBlockOf(4).startingAfter(node4);
        assertThat(children, is(notNullValue()));
        assertThat(children, hasChildren(child("node5"), child("node6"), child("node7"), child("node8")));

        children = graph.getChildren().inBlockOf(3).startingAfter(node4);
        assertThat(children, is(notNullValue()));
        assertThat(children, hasChildren(child("node5"), child("node6"), child("node7")));

        Location node7 = allChildren.get(6);
        children = graph.getChildren().inBlockOf(10).startingAfter(node7);
        assertThat(children, is(notNullValue()));
        assertThat(children, hasChildren(child("node8"), child("node9"), child("node10")));
    }

    protected int createTree( String initialPath,
                              int numberPerDepth,
                              int depth,
                              int numProps,
                              String description,
                              boolean oneBatch,
                              boolean printIntermediate ) {
        long totalNumber = numberNodesInTree(numberPerDepth, depth) - 1; // this method doesn't create the root
        if (description == null) description = "" + numberPerDepth + "x" + depth + " tree with " + numProps
                                               + " properties per node" + (compressData ? " (compressed)" : "");
        System.out.println(description + " (" + totalNumber + " nodes):");
        long totalNumberCreated = 0;
        String batchStr = "batch ";
        Graph.Batch batch = oneBatch ? graph.batch() : null;
        Stopwatch sw = new Stopwatch();
        if (batch != null) {
            totalNumberCreated += createChildren(batch, initialPath, "node", numberPerDepth, numProps, depth, printIntermediate);
            sw.start();
            batch.execute();
        } else {
            batchStr = "";
            sw.start();
            totalNumberCreated += createChildren(null, initialPath, "node", numberPerDepth, numProps, depth, printIntermediate);
        }
        sw.stop();
        long totalDurationInMicroseconds = TimeUnit.NANOSECONDS.toMicros(sw.getTotalDuration().longValue());
        long avgDuration = totalDurationInMicroseconds / totalNumber / 1000L;
        String units = " millisecond(s)";
        if (avgDuration == 0L) {
            avgDuration = totalDurationInMicroseconds / totalNumber;
            units = " microsecond(s)";
        }
        System.out.println("     Total = " + sw.getTotalDuration() + "; avg = " + avgDuration + units);

        // Perform second batch ...
        batch = graph.batch();
        totalNumberCreated += createChildren(batch, initialPath, "secondBranch", 2, numProps, 2, printIntermediate);
        sw = new Stopwatch();
        sw.start();
        batch.execute();
        sw.stop();
        totalDurationInMicroseconds = TimeUnit.NANOSECONDS.toMicros(sw.getTotalDuration().longValue());
        avgDuration = totalDurationInMicroseconds / totalNumber / 1000L;
        units = " millisecond(s)";
        if (avgDuration == 0L) {
            avgDuration = totalDurationInMicroseconds / totalNumber;
            units = " microsecond(s)";
        }
        System.out.println("     Final " + batchStr + "total = " + sw.getTotalDuration() + "; avg = " + avgDuration + units);
        assertThat(totalNumberCreated, is(totalNumber + numberNodesInTree(2, 2) - 1));
        return (int)totalNumberCreated;
    }

    protected int createChildren( Graph.Batch useBatch,
                                  String parentPath,
                                  String nodePrefix,
                                  int number,
                                  int numProps,
                                  int depthRemaining,
                                  boolean printIntermediateStatus ) {
        int numberCreated = 0;
        Graph.Batch batch = useBatch;
        if (batch == null) batch = graph.batch();
        for (int i = 0; i != number; ++i) {
            String path = parentPath + "/" + nodePrefix + (i + 1);
            Graph.Create<Graph.Batch> create = batch.create(path);
            String originalValue = "The quick brown fox jumped over the moon. What? ";
            String value = originalValue;
            for (int j = 0; j != numProps; ++j) {
                // value = value + originalValue;
                create = create.with("property" + (j + 1), value);
            }
            create.and();
        }
        numberCreated += number;
        if (useBatch == null) {
            batch.execute();
            if (printIntermediateStatus) {
                System.out.println("         total created ... " + numberCreated);
            }
        }
        if (depthRemaining > 1) {
            for (int i = 0; i != number; ++i) {
                String path = parentPath + "/" + nodePrefix + (i + 1);
                numberCreated += createChildren(useBatch, path, nodePrefix, number, numProps, depthRemaining - 1, false);
                if (printIntermediateStatus) {
                    System.out.println("         total created ... " + numberCreated);
                }
            }
        }
        return numberCreated;
    }

    protected int numberNodesInTree( int numberPerDepth,
                                     int depth ) {
        assert depth > 0;
        assert numberPerDepth > 0;
        int totalNumber = 0;
        for (int i = 0; i <= depth; ++i) {
            totalNumber += (int)Math.pow(numberPerDepth, i);
        }
        return totalNumber;
    }

}
