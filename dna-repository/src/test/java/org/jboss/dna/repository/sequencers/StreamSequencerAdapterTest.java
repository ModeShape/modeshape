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
package org.jboss.dna.repository.sequencers;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import org.jboss.dna.common.collection.Problem;
import org.jboss.dna.common.jcr.AbstractJcrRepositoryTest;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.common.monitor.RecordingProgressMonitor;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.sequencers.SequencerContext;
import org.jboss.dna.graph.sequencers.SequencerOutput;
import org.jboss.dna.graph.sequencers.StreamSequencer;
import org.jboss.dna.repository.observation.NodeChange;
import org.jboss.dna.repository.util.JcrExecutionContext;
import org.jboss.dna.repository.util.JcrNamespaceRegistry;
import org.jboss.dna.repository.util.JcrTools;
import org.jboss.dna.repository.util.RepositoryNodePath;
import org.jboss.dna.repository.util.SessionFactory;
import org.jboss.dna.repository.util.BasicJcrExecutionContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class StreamSequencerAdapterTest extends AbstractJcrRepositoryTest {

    private StreamSequencer streamSequencer;
    private StreamSequencerAdapter sequencer;
    private String[] validExpressions = {"/a/* => /output"};
    private SequencerConfig validConfig = new SequencerConfig("name", "desc", "something.class", null, validExpressions);
    private JcrTools tools;
    private Session session;
    private SequencerOutputMap sequencerOutput;
    private String sampleData = "The little brown fox didn't something bad.";
    private JcrExecutionContext context;
    private RecordingProgressMonitor progressMonitor;
    private String repositoryWorkspaceName = "something";

    @Before
    public void beforeEach() {
        final JcrTools tools = new JcrTools();
        this.tools = tools;
        final SessionFactory sessionFactory = new SessionFactory() {

            public Session createSession( String name ) {
                return createTestSession();
            }
        };
        NamespaceRegistry registry = new JcrNamespaceRegistry(sessionFactory, "doesn't matter");
        this.context = new BasicJcrExecutionContext(sessionFactory, registry, null, null);
        this.sequencerOutput = new SequencerOutputMap(this.context.getValueFactories());
        this.progressMonitor = new RecordingProgressMonitor(StreamSequencerAdapterTest.class.getName());
        final SequencerOutputMap finalOutput = sequencerOutput;
        this.streamSequencer = new StreamSequencer() {

            /**
             * This method always copies the {@link StreamSequencerAdapterTest#sequencerOutput} data into the output {@inheritDoc},
             * and does nothing else with any of the other parameters.
             */
            public void sequence( InputStream stream,
                                  SequencerOutput output,
                                  SequencerContext context,
                                  ProgressMonitor progressMonitor ) {
                for (SequencerOutputMap.Entry entry : finalOutput) {
                    Path nodePath = entry.getPath();
                    for (SequencerOutputMap.PropertyValue property : entry.getPropertyValues()) {
                        output.setProperty(nodePath, property.getName(), property.getValue());
                    }
                }
            }
        };
        sequencer = new StreamSequencerAdapter(streamSequencer);
    }

    @After
    public void afterEach() {
        if (session != null) {
            try {
                session.logout();
            } finally {
                session = null;
            }
        }
    }

    protected Session createTestSession() {
        try {
            return getRepository().login(getTestCredentials());
        } catch (Exception e) {
            fail("Unable to create repository session: " + e.getMessage());
            return null; // won't get here
        }
    }

    protected void testSequencer( final StreamSequencer sequencer ) throws Exception {
        StreamSequencer streamSequencer = new StreamSequencer() {

            public void sequence( InputStream stream,
                                  SequencerOutput output,
                                  SequencerContext context,
                                  ProgressMonitor progressMonitor ) {
                try {
                    sequencer.sequence(stream, output, context, progressMonitor);
                } catch (AssertionError err) {
                    progressMonitor.getProblems().addError(err, null);
                }
            }
        };
        StreamSequencerAdapter adapter = new StreamSequencerAdapter(streamSequencer);
        startRepository();
        session = getRepository().login(getTestCredentials());
        Node inputNode = tools.findOrCreateNode(session, "/a/b/c");
        Node outputNode = tools.findOrCreateNode(session, "/d/e");
        inputNode.setProperty("sequencedProperty", new ByteArrayInputStream(sampleData.getBytes()));
        NodeChange nodeChange = new NodeChange(repositoryWorkspaceName, inputNode.getPath(), Event.PROPERTY_CHANGED,
                                               Collections.singleton("sequencedProperty"), null);
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositoryWorkspaceName, outputNode.getPath()));
        sequencerOutput.setProperty("alpha/beta", "isSomething", true);
        adapter.execute(inputNode, "sequencedProperty", nodeChange, outputPaths, context, progressMonitor);
        for (Problem problem : progressMonitor.getProblems()) {
            throw (AssertionError)problem.getThrowable();
        }
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
        startRepository();
        session = getRepository().login(getTestCredentials());

        // Set up the repository for the test ...
        Node nodeC = tools.findOrCreateNode(session, "/a/b/c");
        Node nodeE = tools.findOrCreateNode(session, "/d/e");
        assertThat(nodeC, is(notNullValue()));
        assertThat(nodeE, is(notNullValue()));
        assertThat(nodeE.getNodes().getSize(), is(0l));
        assertThat(nodeE.getProperties().getSize(), is(1l)); // jcr:primaryType
        assertThat(nodeE.getProperty("jcr:primaryType").getString(), is("nt:unstructured"));

        // Set the property that will be sequenced ...
        nodeC.setProperty("sequencedProperty", new ByteArrayInputStream(sampleData.getBytes()));

        // Set up the node changes ...
        NodeChange nodeChange = new NodeChange(repositoryWorkspaceName, nodeC.getPath(), Event.PROPERTY_CHANGED,
                                               Collections.singleton("sequencedProperty"), null);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositoryWorkspaceName, nodeE.getPath()));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty("alpha/beta", "isSomething", true);

        // Call the sequencer ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, context, progressMonitor);
    }

    @Test( expected = SequencerException.class )
    public void shouldExecuteSequencerOnExistingNodeWithMissingSequencedPropertyAndOutputToExistingNode() throws Exception {
        startRepository();
        session = getRepository().login(getTestCredentials());

        // Set up the repository for the test ...
        Node nodeC = tools.findOrCreateNode(session, "/a/b/c");
        Node nodeE = tools.findOrCreateNode(session, "/d/e");
        assertThat(nodeC, is(notNullValue()));
        assertThat(nodeE, is(notNullValue()));
        assertThat(nodeE.getNodes().getSize(), is(0l));
        assertThat(nodeE.getProperties().getSize(), is(1l)); // jcr:primaryType
        assertThat(nodeE.getProperty("jcr:primaryType").getString(), is("nt:unstructured"));

        // Set the property that will be sequenced ...
        // THIS TEST REQUIRES THIS PROPERTY TO BE NULL OR NON-EXISTANT
        nodeC.setProperty("sequencedProperty", (InputStream)null);

        // Set up the node changes ...
        NodeChange nodeChange = new NodeChange(repositoryWorkspaceName, nodeC.getPath(), Event.PROPERTY_CHANGED,
                                               Collections.singleton("sequencedProperty"), null);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositoryWorkspaceName, nodeE.getPath()));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty("alpha/beta", "isSomething", true);

        // Call the sequencer, which should cause the exception ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, context, progressMonitor);
    }

    @Test
    public void shouldExecuteSequencerOnExistingNodeAndOutputToMultipleExistingNodes() throws Exception {
        startRepository();
        session = getRepository().login(getTestCredentials());

        // Set up the repository for the test ...
        Node nodeC = tools.findOrCreateNode(session, "/a/b/c");
        Node nodeE = tools.findOrCreateNode(session, "/d/e");
        assertThat(nodeC, is(notNullValue()));
        assertThat(nodeE, is(notNullValue()));
        assertThat(nodeE.getNodes().getSize(), is(0l));
        assertThat(nodeE.getProperties().getSize(), is(1l)); // jcr:primaryType
        assertThat(nodeE.getProperty("jcr:primaryType").getString(), is("nt:unstructured"));

        // Set the property that will be sequenced ...
        nodeC.setProperty("sequencedProperty", new ByteArrayInputStream(sampleData.getBytes()));

        // Set up the node changes ...
        NodeChange nodeChange = new NodeChange(repositoryWorkspaceName, nodeC.getPath(), Event.PROPERTY_CHANGED,
                                               Collections.singleton("sequencedProperty"), null);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositoryWorkspaceName, "/d/e"));
        outputPaths.add(new RepositoryNodePath(repositoryWorkspaceName, "/x/y/z"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty("alpha/beta", "isSomething", true);

        // Call the sequencer ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, context, progressMonitor);

        // Check to see that the output nodes have been created ...
        assertThat(session.getRootNode().hasNode("d/e"), is(true));
        assertThat(session.getRootNode().hasNode("x/y/z"), is(true));
    }

    @Test
    public void shouldExecuteSequencerOnExistingNodeAndOutputToNonExistingNode() throws Exception {
        startRepository();
        session = getRepository().login(getTestCredentials());

        // Set up the repository for the test ...
        Node nodeC = tools.findOrCreateNode(session, "/a/b/c");
        assertThat(session.getRootNode().hasNode("d"), is(false));
        assertThat(nodeC, is(notNullValue()));

        // Set the property that will be sequenced ...
        nodeC.setProperty("sequencedProperty", new ByteArrayInputStream(sampleData.getBytes()));

        // Set up the node changes ...
        NodeChange nodeChange = new NodeChange(repositoryWorkspaceName, nodeC.getPath(), Event.PROPERTY_CHANGED,
                                               Collections.singleton("sequencedProperty"), null);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositoryWorkspaceName, "/d/e"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty("alpha/beta", "isSomething", true);

        // Call the sequencer ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, context, progressMonitor);

        // Check to see that the "/d/e" node has been created ...
        assertThat(session.getRootNode().hasNode("d/e"), is(true));
    }

    @Test
    public void shouldExecuteSequencerOnExistingNodeAndOutputToMultipleNonExistingNodes() throws Exception {
        startRepository();
        session = getRepository().login(getTestCredentials());

        // Set up the repository for the test ...
        Node nodeC = tools.findOrCreateNode(session, "/a/b/c");
        assertThat(session.getRootNode().hasNode("d"), is(false));
        assertThat(session.getRootNode().hasNode("x"), is(false));
        assertThat(nodeC, is(notNullValue()));

        // Set the property that will be sequenced ...
        nodeC.setProperty("sequencedProperty", new ByteArrayInputStream(sampleData.getBytes()));

        // Set up the node changes ...
        NodeChange nodeChange = new NodeChange(repositoryWorkspaceName, nodeC.getPath(), Event.PROPERTY_CHANGED,
                                               Collections.singleton("sequencedProperty"), null);

        // Set up the output directory ...
        Set<RepositoryNodePath> outputPaths = new HashSet<RepositoryNodePath>();
        outputPaths.add(new RepositoryNodePath(repositoryWorkspaceName, "/d/e"));
        outputPaths.add(new RepositoryNodePath(repositoryWorkspaceName, "/x/y/z"));
        outputPaths.add(new RepositoryNodePath(repositoryWorkspaceName, "/x/z"));

        // Generate the output data that the sequencer subclass will produce and that should be saved to the repository ...
        sequencerOutput.setProperty("alpha/beta", "isSomething", true);

        // Call the sequencer ...
        sequencer.execute(nodeC, "sequencedProperty", nodeChange, outputPaths, context, progressMonitor);

        // Check to see that the output nodes have been created ...
        assertThat(session.getRootNode().hasNode("d/e"), is(true));
        assertThat(session.getRootNode().hasNode("x/y/z"), is(true));
        assertThat(session.getRootNode().hasNode("x/z"), is(true));

        // Check to see that the sequencer-generated nodes have been created ...
        // Node beta = session.getRootNode().getNode("d/e/alpha/beta");
        // for (PropertyIterator iter = beta.getProperties(); iter.hasNext();) {
        // Property property = iter.nextProperty();
        // System.out.println("Property on " + beta.getPath() + " ===> " + property.getName() + " = " + property.getValue());
        // }
        assertThat(session.getRootNode().getNode("d/e/alpha/beta").getProperty("isSomething").getBoolean(), is(true));
        assertThat(session.getRootNode().getNode("x/y/z/alpha/beta").getProperty("isSomething").getBoolean(), is(true));
        assertThat(session.getRootNode().getNode("x/z/alpha/beta").getProperty("isSomething").getBoolean(), is(true));
    }

    @Test
    public void shouldSequencerOutputProvideAccessToNamespaceRegistry() {
        assertThat(sequencerOutput.getNamespaceRegistry(), notNullValue());
    }

    @Test
    public void shouldPassNonNullInputStreamToSequencer() throws Exception {
        testSequencer(new StreamSequencer() {

            public void sequence( InputStream stream,
                                  SequencerOutput output,
                                  SequencerContext context,
                                  ProgressMonitor progressMonitor ) {
                assertThat(stream, notNullValue());
            }
        });
    }

    @Test
    public void shouldPassNonNullSequencerOutputToSequencer() throws Exception {
        testSequencer(new StreamSequencer() {

            public void sequence( InputStream stream,
                                  SequencerOutput output,
                                  SequencerContext context,
                                  ProgressMonitor progressMonitor ) {
                assertThat(output, notNullValue());
            }
        });
    }

    @Test
    public void shouldPassNonNullSequencerContextToSequencer() throws Exception {
        testSequencer(new StreamSequencer() {

            public void sequence( InputStream stream,
                                  SequencerOutput output,
                                  SequencerContext context,
                                  ProgressMonitor progressMonitor ) {
                assertThat(context, notNullValue());
            }
        });
    }

    @Test
    public void shouldPassNonNullProgressMonitorToSequencer() throws Exception {
        testSequencer(new StreamSequencer() {

            public void sequence( InputStream stream,
                                  SequencerOutput output,
                                  SequencerContext context,
                                  ProgressMonitor progressMonitor ) {
                assertThat(progressMonitor, notNullValue());
            }
        });
    }
}
