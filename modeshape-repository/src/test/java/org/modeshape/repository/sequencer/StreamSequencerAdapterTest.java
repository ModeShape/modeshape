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
package org.modeshape.repository.sequencer;

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
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.observe.NetChangeObserver.ChangeType;
import org.modeshape.graph.observe.NetChangeObserver.NetChange;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.repository.ModeShapeLexicon;
import org.modeshape.repository.util.RepositoryNodePath;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class StreamSequencerAdapterTest {

    private StreamSequencer streamSequencer;
    private StreamSequencerAdapter sequencer;
    private final String[] validExpressions = {"/a/* => /output"};
    private final SequencerConfig validConfig = new SequencerConfig("name", "desc", Collections.<String, Object>emptyMap(),
                                                                    "something.class", null, validExpressions);
    private SequencerOutputMap sequencerOutput;
    private final String sampleData = "The little brown fox didn't something bad.";
    private ExecutionContext context;
    private SequencerContext seqContext;
    private final String repositorySourceName = "repository";
    private final String repositoryWorkspaceName = "";
    private Problems problems;
    private Graph graph;
    private Property sequencedProperty;
    private DateTime now;

    @Before
    public void beforeEach() {
        problems = new SimpleProblems();
        this.context = new ExecutionContext();
        this.now = this.context.getValueFactories().getDateFactory().create();
        this.sequencerOutput = new SequencerOutputMap(this.context.getValueFactories());
        final SequencerOutputMap finalOutput = sequencerOutput;

        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName(repositorySourceName);
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
        sequencer = new StreamSequencerAdapter(streamSequencer, false);
        seqContext = new SequencerContext(context, graph, graph, now);
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Node assertNodeDoesExist( Graph graph,
                                        String path ) {
        Node node = graph.getNodeAt(path);
        assertThat(node, is(notNullValue()));
        return node;
    }

    protected void assertNodeDoesNotExist( Graph graph,
                                           String path ) {
        try {
            graph.getNodeAt(path);
            fail();
        } catch (PathNotFoundException pnfe) {
            // Expected
        }
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

        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
        graph.create("/d").and().create("/d/e").and();
        graph.set("sequencedProperty").on("/a/b/c").to(new ByteArrayInputStream(sampleData.getBytes()));
        Node inputNode = graph.getNodeAt("/a/b/c");

        Location location = Location.create(context.getValueFactories().getPathFactory().create("/a/b/c"));
        Property sequencedProperty = inputNode.getProperty("sequencedProperty");
        NetChange nodeChange = new NetChange(repositoryWorkspaceName, location, EnumSet.of(ChangeType.PROPERTY_CHANGED), null,
                                             Collections.singleton(sequencedProperty), null, null, null, false);
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/d/e"));
        sequencerOutput.setProperty(path("alpha/beta"), name("isSomething"), true);
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
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
        graph.create("/d").and().create("/d/e").and();
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
        NetChange nodeChange = new NetChange(repositoryWorkspaceName, location, EnumSet.of(ChangeType.PROPERTY_CHANGED), null,
                                             Collections.singleton(sequencedProperty), null, null, null, false);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/d/e"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty(path("alpha/beta"), name("isSomething"), true);

        // Call the sequencer ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, seqContext, problems);
    }

    @Test( expected = SequencerException.class )
    public void shouldExecuteSequencerOnExistingNodeWithMissingSequencedPropertyAndOutputToExistingNode() throws Exception {

        // Set up the repository for the test ...
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
        graph.create("/d").and().create("/d/e").and();
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
        NetChange nodeChange = new NetChange(repositoryWorkspaceName, location, EnumSet.of(ChangeType.PROPERTY_CHANGED), null,
                                             Collections.singleton(sequencedProperty), null, null, null, false);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/d/e"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty(path("alpha/beta"), name("isSomething"), true);

        // Call the sequencer, which should cause the exception ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, seqContext, problems);
    }

    @Test
    public void shouldExecuteSequencerOnExistingNodeAndOutputToMultipleExistingNodes() throws Exception {

        // Set up the repository for the test ...
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
        graph.create("/d").and().create("/d/e").and();

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
        NetChange nodeChange = new NetChange(repositoryWorkspaceName, location, EnumSet.of(ChangeType.PROPERTY_CHANGED), null,
                                             Collections.singleton(sequencedProperty), null, null, null, false);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/d/e"));
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/x/y/z"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty(path("alpha/beta"), name("isSomething"), true);

        // Call the sequencer ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, seqContext, problems);

        // Check to see that the output nodes have been created ...
        assertThat(graph.getNodeAt("/d/e"), is(notNullValue()));
        assertThat(graph.getNodeAt("/x/y/z"), is(notNullValue()));
    }

    @Test
    public void shouldExecuteSequencerOnExistingNodeAndOutputToNonExistingNode() throws Exception {

        // Set up the repository for the test ...
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();

        // Set the property that will be sequenced ...
        graph.set("sequencedProperty").on("/a/b/c").to(new ByteArrayInputStream(sampleData.getBytes()));

        Node nodeC = graph.getNodeAt("/a/b/c");
        assertThat(nodeC, is(notNullValue()));
        assertNodeDoesNotExist(graph, "/d");

        // Set up the node changes ...
        Location location = Location.create(context.getValueFactories().getPathFactory().create("/a/b/c"));
        Property sequencedProperty = nodeC.getProperty("sequencedProperty");
        NetChange nodeChange = new NetChange(repositoryWorkspaceName, location, EnumSet.of(ChangeType.PROPERTY_CHANGED), null,
                                             Collections.singleton(sequencedProperty), null, null, null, false);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/d/e"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty(path("alpha/beta"), name("isSomething"), true);

        // Call the sequencer ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, seqContext, problems);

        // Check to see that the "/d/e" node has been created ...
        assertThat(graph.getNodeAt("/d/e"), is(notNullValue()));
    }

    @Test
    public void shouldExecuteSequencerOnExistingNodeAndOutputToMultipleNonExistingNodes() throws Exception {

        // Set up the repository for the test ...
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();

        // Set the property that will be sequenced ...
        graph.set("sequencedProperty").on("/a/b/c").to(new ByteArrayInputStream(sampleData.getBytes()));

        Node nodeC = assertNodeDoesExist(graph, "/a/b/c");
        assertNodeDoesNotExist(graph, "/d");
        assertNodeDoesNotExist(graph, "/x");

        // Set up the node changes ...
        Location location = Location.create(context.getValueFactories().getPathFactory().create("/a/b/c"));
        Property sequencedProperty = nodeC.getProperty("sequencedProperty");
        NetChange nodeChange = new NetChange(repositoryWorkspaceName, location, EnumSet.of(ChangeType.PROPERTY_CHANGED), null,
                                             Collections.singleton(sequencedProperty), null, null, null, false);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/d/e"));
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/x/y/z"));
        outputPaths.add(new RepositoryNodePath(repositorySourceName, repositoryWorkspaceName, "/x/z"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty(path("alpha/beta"), name("isSomething"), true);

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

    @Test
    public void shouldProvideNamespaceRegistry() throws Exception {

        this.sequencedProperty = mock(Property.class);
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
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
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
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
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
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
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
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
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
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
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
        Node input = graph.getNodeAt("/a/b/c");
        StreamSequencerContext sequencerContext = sequencer.createStreamSequencerContext(input,
                                                                                         sequencedProperty,
                                                                                         seqContext,
                                                                                         problems);
        assertThat(sequencerContext.getMimeType(), is("text/plain"));
        assertThat(context.getMimeTypeDetector().mimeTypeOf("myFile.rdf", null), is("application/rdf+xml"));
        assertThat(context.getMimeTypeDetector().mimeTypeOf("c", null), is("text/plain"));
    }

    @Test
    public void shouldCreateSequencerContextThatProvidesMimeTypeForJcrFileNodeStructure() throws Exception {

        this.sequencedProperty = mock(Property.class);
        graph.create("/a").and().create("/a/myFile.rdf").and().create("/a/myFile.rdf/jcr:content").and();
        Node input = graph.getNodeAt("/a/myFile.rdf/jcr:content");
        StreamSequencerContext sequencerContext = sequencer.createStreamSequencerContext(input,
                                                                                         sequencedProperty,
                                                                                         seqContext,
                                                                                         problems);
        assertThat(sequencerContext.getMimeType(), is("application/rdf+xml"));
        assertThat(context.getMimeTypeDetector().mimeTypeOf("myFile.rdf", null), is("application/rdf+xml"));
        assertThat(context.getMimeTypeDetector().mimeTypeOf("jcr:content", null), is("text/plain"));
    }

    @Test
    public void shouldCreateSequencerContextThatProvidesMimeTypeForNonJcrFileNodeStructure() throws Exception {

        this.sequencedProperty = mock(Property.class);
        graph.create("/a").and().create("/a/myFile.rdf").and().create("/a/myFile.rdf/content").and();
        Node input = graph.getNodeAt("/a/myFile.rdf/content");
        StreamSequencerContext sequencerContext = sequencer.createStreamSequencerContext(input,
                                                                                         sequencedProperty,
                                                                                         seqContext,
                                                                                         problems);
        assertThat(sequencerContext.getMimeType(), is("text/plain"));
        assertThat(context.getMimeTypeDetector().mimeTypeOf("myFile.rdf", null), is("application/rdf+xml"));
        assertThat(context.getMimeTypeDetector().mimeTypeOf("content", null), is("text/plain"));
    }

    private Name nameFor( String raw ) {
        return context.getValueFactories().getNameFactory().create(raw);
    }

    @Test
    public void shouldNotCreateExtraNodesWhenSavingOutput() throws Exception {
        SequencerOutputMap output = new SequencerOutputMap(context.getValueFactories());
        Map<Name, Property> props = null;

        /*
         * Create several output properties and make sure the resulting graph
         * does not contain duplicate nodes
         */
        output.setProperty(path("a"), name("property1"), "value1");
        output.setProperty(path("a/b"), name("property1"), "value1");
        output.setProperty(path("a/b"), name("property2"), "value2");
        output.setProperty(path("a/b[2]"), name("property1"), "value1");
        output.setProperty(path("a/b[2]/c"), name("property1"), "value1");

        Set<Path> builtPaths = new HashSet<Path>();
        sequencer.saveOutput(path("/input/path"), "/", output, seqContext, builtPaths, true);
        seqContext.getDestination().submit();

        Node rootNode = graph.getNodeAt("/");
        assertThat(rootNode.getChildren().size(), is(1));

        Node nodeA = graph.getNodeAt("/a");
        props = nodeA.getPropertiesByName();

        assertThat(nodeA.getChildren().size(), is(2));
        assertThat(props.size(), is(2)); // Need to add one to account for dna:uuid
        assertThat(props.get(nameFor("property1")).getFirstValue().toString(), is("value1"));

        Node nodeB = graph.getNodeAt("/a/b[1]");
        props = nodeB.getPropertiesByName();

        assertThat(props.size(), is(3)); // Need to add one to account for dna:uuid
        assertThat(props.get(nameFor("property1")).getFirstValue().toString(), is("value1"));
        assertThat(props.get(nameFor("property2")).getFirstValue().toString(), is("value2"));

        Node nodeB2 = graph.getNodeAt("/a/b[2]");
        props = nodeB2.getPropertiesByName();

        assertThat(props.size(), is(2)); // Need to add one to account for dna:uuid
        assertThat(props.get(nameFor("property1")).getFirstValue().toString(), is("value1"));

        Node nodeC = graph.getNodeAt("/a/b[2]/c");
        props = nodeC.getPropertiesByName();

        assertThat(props.size(), is(2)); // Need to add one to account for dna:uuid
        assertThat(props.get(nameFor("property1")).getFirstValue().toString(), is("value1"));

    }

    @FixFor( "MODE-1012" )
    @Test
    public void shouldSequenceInputFromOneGraphAndSaveOutputToAnotherGraph() throws Exception {
        // Set up the second source ...
        String repositorySourceName2 = "repository2";
        InMemoryRepositorySource source2 = new InMemoryRepositorySource();
        source2.setName(repositorySourceName2);
        Graph graph2 = Graph.create(source2.getConnection(), context);
        seqContext = new SequencerContext(context, graph, graph2, now);

        // Set up the node that will be sequenced ...
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and();
        graph.set("sequencedProperty").on("/a/b/c").to(new ByteArrayInputStream(sampleData.getBytes()));

        Node nodeC = assertNodeDoesExist(graph, "/a/b/c");
        assertNodeDoesNotExist(graph, "/d");
        assertNodeDoesNotExist(graph, "/x");

        // Set up the node changes ...
        Location location = Location.create(context.getValueFactories().getPathFactory().create("/a/b/c"));
        Property sequencedProperty = nodeC.getProperty("sequencedProperty");
        NetChange nodeChange = new NetChange(repositoryWorkspaceName, location, EnumSet.of(ChangeType.PROPERTY_CHANGED), null,
                                             Collections.singleton(sequencedProperty), null, null, null, false);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositorySourceName2, repositoryWorkspaceName, "/d/e"));
        outputPaths.add(new RepositoryNodePath(repositorySourceName2, repositoryWorkspaceName, "/x/y/z"));
        outputPaths.add(new RepositoryNodePath(repositorySourceName2, repositoryWorkspaceName, "/x/z"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty(path("alpha/beta"), name("isSomething"), true);

        // Call the sequencer ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, seqContext, problems);

        // Check to see that the output nodes have been created ...
        assertThat(graph2.getNodeAt("/d/e"), is(notNullValue()));
        assertThat(graph2.getNodeAt("/x/y/z"), is(notNullValue()));
        assertThat(graph2.getNodeAt("/x/z"), is(notNullValue()));

        // Check to see that the sequencer-generated nodes have been created ...
        assertThat(graph2.getNodeAt("/d/e/alpha/beta").getProperty("isSomething").getFirstValue().toString(), is("true"));
        assertThat(graph2.getNodeAt("/x/y/z/alpha/beta").getProperty("isSomething").getFirstValue().toString(), is("true"));
        assertThat(graph2.getNodeAt("/x/z/alpha/beta").getProperty("isSomething").getFirstValue().toString(), is("true"));

    }

    @FixFor( "MODE-1033" )
    @Test
    public void shouldAddDerivedMixinAndDerivedFromPropertyToRootNodeOfSequencedOutput() {
        sequencer = new StreamSequencerAdapter(streamSequencer, true);
        SequencerOutputMap output = new SequencerOutputMap(context.getValueFactories());
        Map<Name, Property> props = null;

        /*
         * Create several output properties and make sure the resulting graph
         * does not contain duplicate nodes
         */
        output.setProperty(path("a"), name("property1"), "value1");
        output.setProperty(path("a/b"), name("property1"), "value1");
        output.setProperty(path("a/b"), name("property2"), "value2");
        output.setProperty(path("a/b[2]"), name("property1"), "value1");
        output.setProperty(path("a/b[2]/c"), name("property1"), "value1");

        Set<Path> builtPaths = new HashSet<Path>();
        Path inputPath = path("/input/path");
        sequencer.saveOutput(inputPath, "/", output, seqContext, builtPaths, true);
        seqContext.getDestination().submit();

        Node rootNode = graph.getNodeAt("/");
        assertThat(rootNode.getChildren().size(), is(1));

        Node nodeA = graph.getNodeAt("/a");
        props = nodeA.getPropertiesByName();

        assertThat(nodeA.getChildren().size(), is(2));
        assertThat(props.size(), is(5)); // Need to add one to account for dna:uuid, jcr:mixinTypes, mode:derivedFrom,
        // mode:derivedAt
        assertThat(props.get(nameFor("property1")).getFirstValue().toString(), is("value1"));
        assertThat(props.get(JcrLexicon.MIXIN_TYPES).getFirstValue(), is((Object)ModeShapeLexicon.DERIVED));
        assertThat(props.get(ModeShapeLexicon.DERIVED_FROM).getFirstValue(), is((Object)inputPath));
        assertThat(props.get(ModeShapeLexicon.DERIVED_AT).getFirstValue(), is((Object)now));

        Node nodeB = graph.getNodeAt("/a/b[1]");
        props = nodeB.getPropertiesByName();

        assertThat(props.size(), is(3)); // Need to add one to account for dna:uuid
        assertThat(props.get(nameFor("property1")).getFirstValue().toString(), is("value1"));
        assertThat(props.get(nameFor("property2")).getFirstValue().toString(), is("value2"));

        Node nodeB2 = graph.getNodeAt("/a/b[2]");
        props = nodeB2.getPropertiesByName();

        assertThat(props.size(), is(2)); // Need to add one to account for dna:uuid
        assertThat(props.get(nameFor("property1")).getFirstValue().toString(), is("value1"));

        Node nodeC = graph.getNodeAt("/a/b[2]/c");
        props = nodeC.getPropertiesByName();

        assertThat(props.size(), is(2)); // Need to add one to account for dna:uuid
        assertThat(props.get(nameFor("property1")).getFirstValue().toString(), is("value1"));
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
