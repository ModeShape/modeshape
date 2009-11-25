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
package org.jboss.dna.sequencer.ddl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.SubgraphNode;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.io.GraphSequencerOutput;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.sequencer.MockSequencerContext;
import org.jboss.dna.graph.sequencer.StreamSequencerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class DdlSequencerTest {
    private DdlSequencer sequencer;
    private InputStream content;
    private GraphSequencerOutput output;
    private StreamSequencerContext context;
    private Graph graph;

    // private Graph expectedGraph;

    @Before
    public void beforeEach() {
        sequencer = new DdlSequencer();
        context = new MockSequencerContext("/a/myDdlSequencer.cnd");
        context.getNamespaceRegistry().register("jcr", "http://www.jcp.org/jcr/1.0");
        context.getNamespaceRegistry().register("nt", "http://www.jcp.org/jcr/nt/1.0");
        context.getNamespaceRegistry().register("mix", "http://www.jcp.org/jcr/mix/1.0");
        context.getNamespaceRegistry().register("ddl", "http://jboss.org/dna/ddl/1.0");
        context.getNamespaceRegistry().register("derbyddl", "http://jboss.org/dna/ddl/derby/1.0");
        context.getNamespaceRegistry().register("oracleddl", "http://jboss.org/dna/ddl/oracle/1.0");
        context.getNamespaceRegistry().register("postgresddl", "http://jboss.org/dna/ddl/postgres/1.0");
        context.getNamespaceRegistry().register("mysqlddl", "http://jboss.org/dna/ddl/mysql/1.0");

        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("actual");
        graph = Graph.create(source, context);
        // InMemoryRepositorySource expectedSource = new InMemoryRepositorySource();
        // expectedSource.setName("expected");
        // expectedGraph = Graph.create(expectedSource, context);

        output = new GraphSequencerOutput(graph);
    }

    @After
    public void afterEach() throws Exception {
        if (content != null) {
            try {
                content.close();
            } finally {
                content = null;
            }
        }
    }
    
    /**
     * Utility to create a {@link Path.Segment} from a string, where there will be no index
     * 
     * @param name the string form of the path segment, which may include a 1-based same-name-sibling index
     * @return the path object
     * @throws ValueFormatException if a path could not be created from the supplied string
     */
    protected Path.Segment segment( String name ) {
        return context.getValueFactories().getPathFactory().createSegment(name);
    }
    
    /**
     * Utility to create a {@link Name} from a string.
     * 
     * @param name the string form of the name
     * @return the name object
     * @throws ValueFormatException if a name could not be created from the supplied string
     */
    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }
    
    /**
     * Utility to create a {@link Path} from a string.
     * 
     * @param path the string form of the path
     * @return the path object
     * @throws ValueFormatException if a path could not be created from the supplied string
     */
    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    @Test
    public void shouldGenerateNodeTypesForCreateTables() throws IOException {
        URL url = this.getClass().getClassLoader().getResource("ddl/createTables.ddl");
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);
    }

    @Test
    public void shouldSequenceCreateTable() throws IOException {
        // CREATE TABLE IDTABLE
        // (
        // IDCONTEXT VARCHAR(20) NOT NULL PRIMARY KEY,
        // NEXTID NUMERIC
        // );
        URL url = this.getClass().getClassLoader().getResource("ddl/create_table.ddl");
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);

        // Map<Name, Property> props = output.getProperties("xxxxxx");
    }

    @Test
    public void shouldSequenceCreateSchema() throws IOException {
        // CREATE SCHEMA hollywood
        // CREATE TABLE films (title varchar(255), release date, producerName varchar(255))
        // CREATE VIEW winners AS
        // SELECT title, release FROM films WHERE producerName IS NOT NULL;
        URL url = this.getClass().getClassLoader().getResource("ddl/create_schema.ddl");
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);

        output.close();

        // File expectedFile = new File("src/test/resources/");
        // if ( expectedFile.exists() ) {
        // expectedGraph.importXmlFrom(expectedFile).into("/");

        //assertThat(graph.getChildren().of("/statements"), hasChildren(segment("hollywood")));

        
        
        Subgraph subgraph = graph.getSubgraphOfDepth(10).at("/");
        assertThat(subgraph, is(notNullValue()));
        //assertThat(subgraph.getNode("/statements"), is(notNullValue()));
        //assertThat(subgraph.getNode(".").getChildren(), hasChildren(segment("statements")));
        //Iterator<SubgraphNode> actualIter = actual.iterator();
        
        SubgraphNode rootNode = subgraph.getNode(".");
        assertThat(rootNode.getChildren().size(), is(1));
        
        Iterator<SubgraphNode> expected = subgraph.iterator();
        while ( expected.hasNext() ) {
            SubgraphNode nextNode = expected.next();
            System.out.println("\n   NODE = " + nextNode);
            System.out.println("       PATH = " + nextNode.getLocation().getPath());
            Map<Name,Property> actualProperties = nextNode.getPropertiesByName();
            for(Name nextKey: actualProperties.keySet()) {
                Property nextValue = actualProperties.get(nextKey);
                System.out.println("             KEY = " + nextKey + "  VALUE = " + nextValue.getFirstValue());
            }
        }
        //SubgraphNode schemaNode = rootNode.getNode(path("ddl:statements"));
        //assertThat(schemaNode.getChildren().size(), is(2));
        

        // Subgraph expected = expectedGraph.getSubgraphOfDepth(10).at("/");
        // // compare subgraphs ...
        // Iterator<SubgraphNode> actualIter = actual.iterator();
        // Iterator<SubgraphNode> expectedIter = expected.iterator();
        // while ( actualIter.hasNext() && expectedIter.hasNext() ) {
        // SubgraphNode actualNode = actualIter.next();
        // SubgraphNode expectedNode = expectedIter.next();
        // // Get properties
        // Map<Name,Property> actualProperties = actualNode.getPropertiesByName();
        // Map<Name,Property> expectedProperties = expectedNode.getPropertiesByName();
        // // The property names should be the same ...
        // Set<Name> actualPropertyNames = actualProperties.keySet();
        // Set<Name> expectedPropertyNames = expectedProperties.keySet();
        // assertThat(actualPropertyNames,is(expectedPropertyNames));
        // // compare the properties ...
        // for ( Name name : actualPropertyNames ) {
        // Property actualProp = actualProperties.get(name);
        // Property expectedProp = expectedProperties.get(name);
        // // etc.
        // }
        // }
        // assertThat(actualIter.hasNext(), is(false));
        // assertThat(expectedIter.hasNext(), is(false));
        //
        // } else {
        // // write out the actual graph to the expected file
        // }
        //System.out.println(subgraph);

        // Map<Property> props = output.getProperties(nodePath)
    }

}
