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
package org.jboss.dna.repository.sequencer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.mimetype.MimeTypeDetectors;
import org.jboss.dna.graph.observe.NetChangeObserver.ChangeType;
import org.jboss.dna.graph.observe.NetChangeObserver.NetChange;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.sequencer.SequencerOutput;
import org.jboss.dna.graph.sequencer.StreamSequencer;
import org.jboss.dna.graph.sequencer.StreamSequencerContext;
import org.jboss.dna.repository.util.RepositoryNodePath;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class StreamSequencerAdapterTest {

    private StreamSequencer streamSequencer;
    private StreamSequencerAdapter sequencer;
    private String[] validExpressions = {"/a/* => /output"};
    private SequencerConfig validConfig = new SequencerConfig("name", "desc", Collections.<String, Object>emptyMap(),
                                                              "something.class", null, validExpressions);
    private SequencerOutputMap sequencerOutput;
    private String sampleData = "The little brown fox didn't something bad.";
    private ExecutionContext context;
    private SequencerContext seqContext;
    private String repositorySourceName = "repository";
    private String repositoryWorkspaceName = "";
    private Problems problems;
    private Graph graph;
    private Property sequencedProperty;

    @Before
    public void beforeEach() {
        problems = new SimpleProblems();
        // Set up the MIME type detectors ...
        this.context = new ExecutionContext().with(new MimeTypeDetectors());
        this.sequencerOutput = new SequencerOutputMap(this.context.getValueFactories());
        final SequencerOutputMap finalOutput = sequencerOutput;

        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("repository");
        graph = Graph.create(source.getConnection(), context);
        this.streamSequencer = new StreamSequencer() {

            /**
             * This method always copies the {@link StreamSequencerAdapterTest#sequencerOutput} data into the output {@inheritDoc}
             * , and does nothing else with any of the other parameters.
             */
            public void sequence( InputStream stream,
                                  SequencerOutput output,
                                  StreamSequencerContext context ) {
                for (SequencerOutputMap.Entry entry : finalOutput) {
                    Path nodePath = entry.getPath();
                    for (SequencerOutputMap.PropertyValue property : entry.getPropertyValues()) {
                        output.setProperty(nodePath, property.getName(), property.getValue());
                    }
                }
            }
        };
        sequencer = new StreamSequencerAdapter(streamSequencer);
        seqContext = new SequencerContext(context, graph);
    }

    protected void testSequencer( final StreamSequencer sequencer ) throws Throwable {
        StreamSequencer streamSequencer = new StreamSequencer() {

            public void sequence( InputStream stream,
                                  SequencerOutput output,
                                  StreamSequencerContext context ) {
                sequencer.sequence(stream, output, context);
            }
        };
        StreamSequencerAdapter adapter = new StreamSequencerAdapter(streamSequencer);

        graph.create("/a").and().create("/a/b").and().create("/a/b/c");
        graph.create("/d").and().create("/d/e");
        graph.set("sequencedProperty").on("/a/b/c").to(new ByteArrayInputStream(sampleData.getBytes()));
        Node inputNode = graph.getNodeAt("/a/b/c");

        Location location = Location.create(context.getValueFactories().getPathFactory().create("/a/b/c"));
        Property sequencedProperty = inputNode.getProperty("sequencedProperty");
        NetChange nodeChange = new NetChange(repositorySourceName, repositoryWorkspaceName, location,
                                             EnumSet.of(ChangeType.PROPERTY_CHANGED), Collections.singleton(sequencedProperty),
                                             null);
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/d/e"));
        sequencerOutput.setProperty("alpha/beta", "isSomething", true);
        adapter.execute(inputNode, "sequencedProperty", nodeChange, outputPaths, seqContext, problems);
    }

    @Test
    public void shouldNotHaveSequencerUponInstantiation() {
        assertThat(sequencer.getConfiguration(), is(nullValue()));
        sequencer.setConfiguration(validConfig);
        assertThat(sequencer.getConfiguration(), is(sameInstance(validConfig)));
    }

    @Test
    public void shouldExtractNullMixinTypesFromNullValue() {
        assertThat(sequencer.extractMixinTypes(null), is(nullValue()));
    }

    @Test
    public void shouldExtractMixinTypesFromStringValue() {
        assertThat(sequencer.extractMixinTypes("value"), is(new String[] {"value"}));
        assertThat(sequencer.extractMixinTypes(""), is(new String[] {""}));
    }

    @Test
    public void shouldExtractMixinTypesFromStringArrayValue() {
        assertThat(sequencer.extractMixinTypes(new String[] {"value1"}), is(new String[] {"value1"}));
        assertThat(sequencer.extractMixinTypes(new String[] {"value1", "value2"}), is(new String[] {"value1", "value2"}));
    }

    @Test
    public void shouldExtractMixinTypesFromStringArrayWithNullValue() {
        assertThat(sequencer.extractMixinTypes(new String[] {"value1", null, "value2"}), is(new String[] {"value1", null,
            "value2"}));
    }

    @Test
    public void shouldExecuteSequencerOnExistingNodeAndOutputToExistingNode() throws Exception {

        // Set up the repository for the test ...
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");
        graph.create("/d").and().create("/d/e");
        graph.set("sequencedProperty").on("/a/b/c").to(new ByteArrayInputStream(sampleData.getBytes()));
        Node nodeC = graph.getNodeAt("/a/b/c");
        Node nodeE = graph.getNodeAt("/d/e");
        assertThat(nodeC, is(notNullValue()));
        assertThat(nodeE, is(notNullValue()));
        assertThat(nodeE.getChildren().size(), is(0));
        assertThat(nodeE.getProperties().size(), is(1)); // jcr:uuid
        // assertThat(nodeE.getProperty("jcr:primaryType").getString(), is("nt:unstructured"));

        // Set the property that will be sequenced ...

        // Set up the node changes ...
        Location location = Location.create(context.getValueFactories().getPathFactory().create("/a/b/c"));
        Property sequencedProperty = nodeC.getProperty("sequencedProperty");
        NetChange nodeChange = new NetChange(repositorySourceName, repositoryWorkspaceName, location,
                                             EnumSet.of(ChangeType.PROPERTY_CHANGED), Collections.singleton(sequencedProperty),
                                             null);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/d/e"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty("alpha/beta", "isSomething", true);

        // Call the sequencer ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, seqContext, problems);
    }

    @Test( expected = SequencerException.class )
    public void shouldExecuteSequencerOnExistingNodeWithMissingSequencedPropertyAndOutputToExistingNode() throws Exception {

        // Set up the repository for the test ...
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");
        graph.create("/d").and().create("/d/e");
        Node nodeC = graph.getNodeAt("/a/b/c");
        Node nodeE = graph.getNodeAt("/d/e");
        assertThat(nodeC, is(notNullValue()));
        assertThat(nodeE, is(notNullValue()));
        assertThat(nodeE.getChildren().size(), is(0));
        assertThat(nodeE.getProperties().size(), is(1)); // jcr:uuid
        // assertThat(nodeE.getProperty("jcr:primaryType").getString(), is("nt:unstructured"));

        // Set the property that will be sequenced ...
        // THIS TEST REQUIRES THIS PROPERTY TO BE NULL OR NON-EXISTANT
        graph.set("sequencedProperty").on(nodeC.getLocation()).to((String)null);

        // Set up the node changes ...
        Location location = Location.create(context.getValueFactories().getPathFactory().create("/a/b/c"));
        Property sequencedProperty = nodeC.getProperty("sequencedProperty");
        NetChange nodeChange = new NetChange(repositorySourceName, repositoryWorkspaceName, location,
                                             EnumSet.of(ChangeType.PROPERTY_CHANGED), Collections.singleton(sequencedProperty),
                                             null);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/d/e"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty("alpha/beta", "isSomething", true);

        // Call the sequencer, which should cause the exception ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, seqContext, problems);
    }

    @Test
    public void shouldExecuteSequencerOnExistingNodeAndOutputToMultipleExistingNodes() throws Exception {

        // Set up the repository for the test ...
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");
        graph.create("/d").and().create("/d/e");

        // Set the property that will be sequenced ...
        graph.set("sequencedProperty").on("/a/b/c").to(new ByteArrayInputStream(sampleData.getBytes()));

        Node nodeC = graph.getNodeAt("/a/b/c");
        Node nodeE = graph.getNodeAt("/d/e");
        assertThat(nodeC, is(notNullValue()));
        assertThat(nodeE, is(notNullValue()));
        assertThat(nodeE.getChildren().size(), is(0));
        assertThat(nodeE.getProperties().size(), is(1)); // jcr:uuid
        // assertThat(nodeE.getProperty("jcr:primaryType").getString(), is("nt:unstructured"));

        // Set up the node changes ...
        Location location = Location.create(context.getValueFactories().getPathFactory().create("/a/b/c"));
        Property sequencedProperty = nodeC.getProperty("sequencedProperty");
        NetChange nodeChange = new NetChange(repositorySourceName, repositoryWorkspaceName, location,
                                             EnumSet.of(ChangeType.PROPERTY_CHANGED), Collections.singleton(sequencedProperty),
                                             null);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/d/e"));
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/x/y/z"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty("alpha/beta", "isSomething", true);

        // Call the sequencer ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, seqContext, problems);

        // Check to see that the output nodes have been created ...
        assertThat(graph.getNodeAt("/d/e"), is(notNullValue()));
        assertThat(graph.getNodeAt("/x/y/z"), is(notNullValue()));
    }

    @Test
    public void shouldExecuteSequencerOnExistingNodeAndOutputToNonExistingNode() throws Exception {

        // Set up the repository for the test ...
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");

        // Set the property that will be sequenced ...
        graph.set("sequencedProperty").on("/a/b/c").to(new ByteArrayInputStream(sampleData.getBytes()));

        Node nodeC = graph.getNodeAt("/a/b/c");
        try {
            graph.getNodeAt("/d");
            fail();
        } catch (PathNotFoundException pnfe) {
            // Expected
        }
        assertThat(nodeC, is(notNullValue()));

        // Set up the node changes ...
        Location location = Location.create(context.getValueFactories().getPathFactory().create("/a/b/c"));
        Property sequencedProperty = nodeC.getProperty("sequencedProperty");
        NetChange nodeChange = new NetChange(repositorySourceName, repositoryWorkspaceName, location,
                                             EnumSet.of(ChangeType.PROPERTY_CHANGED), Collections.singleton(sequencedProperty),
                                             null);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/d/e"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty("alpha/beta", "isSomething", true);

        // Call the sequencer ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, seqContext, problems);

        // Check to see that the "/d/e" node has been created ...
        assertThat(graph.getNodeAt("/d/e"), is(notNullValue()));
    }

    @Test
    public void shouldExecuteSequencerOnExistingNodeAndOutputToMultipleNonExistingNodes() throws Exception {

        // Set up the repository for the test ...
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");

        // Set the property that will be sequenced ...
        graph.set("sequencedProperty").on("/a/b/c").to(new ByteArrayInputStream(sampleData.getBytes()));

        Node nodeC = graph.getNodeAt("/a/b/c");
        try {
            graph.getNodeAt("/d");
            fail();
        } catch (PathNotFoundException pnfe) {
            // Expected
        }
        try {
            graph.getNodeAt("/x");
            fail();
        } catch (PathNotFoundException pnfe) {
            // Expected
        }
        assertThat(nodeC, is(notNullValue()));

        // Set up the node changes ...
        Location location = Location.create(context.getValueFactories().getPathFactory().create("/a/b/c"));
        Property sequencedProperty = nodeC.getProperty("sequencedProperty");
        NetChange nodeChange = new NetChange(repositorySourceName, repositoryWorkspaceName, location,
                                             EnumSet.of(ChangeType.PROPERTY_CHANGED), Collections.singleton(sequencedProperty),
                                             null);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/d/e"));
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/x/y/z"));
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/x/z"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty("alpha/beta", "isSomething", true);

        // Call the sequencer ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, seqContext, problems);

        // Check to see that the output nodes have been created ...
        assertThat(graph.getNodeAt("/d/e"), is(notNullValue()));
        assertThat(graph.getNodeAt("/x/y/z"), is(notNullValue()));
        assertThat(graph.getNodeAt("/x/z"), is(notNullValue()));

        // Check to see that the sequencer-generated nodes have been created ...
        // Node beta = session.getRootNode().getNode("d/e/alpha/beta");
        // for (PropertyIterator iter = beta.getProperties(); iter.hasNext();) {
        // Property property = iter.nextProperty();
        // System.out.println("Property on " + beta.getLocation().getPath() + " ===> " + property.getName() + " = " +
        // property.getValue());
        // }
        assertThat(graph.getNodeAt("/d/e/alpha/beta").getProperty("isSomething").getFirstValue().toString(), is("true"));
        assertThat(graph.getNodeAt("/x/y/z/alpha/beta").getProperty("isSomething").getFirstValue().toString(), is("true"));
        assertThat(graph.getNodeAt("/x/z/alpha/beta").getProperty("isSomething").getFirstValue().toString(), is("true"));
    }

    @Test
    public void shouldSequencerOutputProvideAccessToNamespaceRegistry() {
        assertThat(context.getNamespaceRegistry(), notNullValue());
    }

    @Test
    public void shouldPassNonNullInputStreamToSequencer() throws Throwable {
        testSequencer(new StreamSequencer() {

            public void sequence( InputStream stream,
                                  SequencerOutput output,
                                  StreamSequencerContext context ) {
                assertThat(stream, notNullValue());
            }
        });
    }

    @Test
    public void shouldPassNonNullSequencerOutputToSequencer() throws Throwable {
        testSequencer(new StreamSequencer() {

            public void sequence( InputStream stream,
                                  SequencerOutput output,
                                  StreamSequencerContext context ) {
                assertThat(output, notNullValue());
            }
        });
    }

    @Test
    public void shouldPassNonNullSequencerContextToSequencer() throws Throwable {
        testSequencer(new StreamSequencer() {

            public void sequence( InputStream stream,
                                  SequencerOutput output,
                                  StreamSequencerContext context ) {
                assertThat(context, notNullValue());
            }
        });
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullInputNode() throws Exception {
        sequencer.createStreamSequencerContext(null, sequencedProperty, seqContext, problems);
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullSequencedProperty() throws Exception {

        graph.create("/a");
        Node input = graph.getNodeAt("/a");
        sequencer.createStreamSequencerContext(input, null, seqContext, problems);
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullExecutionContext() throws Exception {

        this.sequencedProperty = mock(Property.class);
        graph.create("/a");
        Node input = graph.getNodeAt("/a");
        sequencer.createStreamSequencerContext(input, sequencedProperty, null, problems);
    }

    @Test
    public void shouldProvideNamespaceRegistry() throws Exception {

        this.sequencedProperty = mock(Property.class);
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");
        Node input = graph.getNodeAt("/a/b/c");
        StreamSequencerContext sequencerContext = sequencer.createStreamSequencerContext(input,
                                                                                         sequencedProperty,
                                                                                         seqContext,
                                                                                         problems);
        assertThat(sequencerContext.getNamespaceRegistry(), notNullValue());
    }

    @Test
    public void shouldProvideValueFactories() throws Exception {

        this.sequencedProperty = mock(Property.class);
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");
        Node input = graph.getNodeAt("/a/b/c");
        StreamSequencerContext sequencerContext = sequencer.createStreamSequencerContext(input,
                                                                                         sequencedProperty,
                                                                                         seqContext,
                                                                                         problems);
        assertThat(sequencerContext.getValueFactories(), notNullValue());
    }

    @Test
    public void shouldProvidePathToInput() throws Exception {

        this.sequencedProperty = mock(Property.class);
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");
        Node input = graph.getNodeAt("/a/b/c");
        StreamSequencerContext sequencerContext = sequencer.createStreamSequencerContext(input,
                                                                                         sequencedProperty,
                                                                                         seqContext,
                                                                                         problems);
        assertThat(sequencerContext.getInputPath(), is(context.getValueFactories().getPathFactory().create("/a/b/c")));
    }

    @Test
    public void shouldNeverReturnNullInputProperties() throws Exception {

        this.sequencedProperty = mock(Property.class);
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");
        Node input = graph.getNodeAt("/a/b/c");
        StreamSequencerContext sequencerContext = sequencer.createStreamSequencerContext(input,
                                                                                         sequencedProperty,
                                                                                         seqContext,
                                                                                         problems);
        assertThat(sequencerContext.getInputProperties(), notNullValue());
        assertThat(sequencerContext.getInputProperties().isEmpty(), is(false));
    }

    @Test
    public void shouldProvideInputProperties() throws Exception {

        this.sequencedProperty = mock(Property.class);
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");
        graph.set("x").on("/a/b/c").to(true);
        graph.set("y").on("/a/b/c").to(Arrays.asList(new String[] {"asdf", "xyzzy"}));
        Node input = graph.getNodeAt("/a/b/c");
        StreamSequencerContext sequencerContext = sequencer.createStreamSequencerContext(input,
                                                                                         sequencedProperty,
                                                                                         seqContext,
                                                                                         problems);
        assertThat(sequencerContext.getInputProperties(), notNullValue());
        assertThat(sequencerContext.getInputProperties().isEmpty(), is(false));
        assertThat(sequencerContext.getInputProperties().size(), is(3));
        // verifyProperty(sequencerContext, "jcr:uuid", /* some UUID */null );
        verifyProperty(sequencerContext, "x", true);
        verifyProperty(sequencerContext, "y", "asdf", "xyzzy");
    }

    @Test
    public void shouldCreateSequencerContextThatProvidesMimeType() throws Exception {

        this.sequencedProperty = mock(Property.class);
        graph.create("/a").and().create("/a/b").and().create("/a/b/c");
        Node input = graph.getNodeAt("/a/b/c");
        StreamSequencerContext sequencerContext = sequencer.createStreamSequencerContext(input,
                                                                                         sequencedProperty,
                                                                                         seqContext,
                                                                                         problems);
        assertThat(sequencerContext.getMimeType(), is("text/plain"));
    }

    private void verifyProperty( StreamSequencerContext context,
                                 String name,
                                 Object... values ) {
        Property prop = context.getInputProperty(context.getValueFactories().getNameFactory().create(name));
        assertThat(prop, notNullValue());
        assertThat(prop.getName(), is(context.getValueFactories().getNameFactory().create(name)));
        assertThat(prop.isEmpty(), is(false));
        assertThat(prop.size(), is(values.length));
        assertThat(prop.isMultiple(), is(values.length > 1));
        assertThat(prop.isSingle(), is(values.length == 1));
        Iterator<?> iter = prop.getValues();
        for (Object val : values) {
            assertThat(iter.hasNext(), is(true));
            assertThat(iter.next(), is(val));
        }
    }

}
