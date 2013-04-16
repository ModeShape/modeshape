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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.InputStream;
import javax.jcr.Node;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Json;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Tests of various sequencing configurations.
 */
public class SequencingTest extends AbstractSequencerTest {

    @Override
    protected void startRepositoryWithConfiguration( String configContent ) throws Exception {
        Document doc = Json.read(configContent);
        startRepositoryWithConfiguration(doc);
    }

    @Override
    protected void startRepositoryWithConfiguration( Document doc ) throws Exception {
        stopRepository();
        config = new RepositoryConfiguration(doc, REPO_NAME, environment);
        repository = new JcrRepository(config);
        repository.start();
        session = repository.login();
        rootNode = session.getRootNode();
        addSequencingListeners(session);
    }

    @Override
    protected void startRepositoryWithConfiguration( InputStream configInputStream ) throws Exception {
        stopRepository();
        config = RepositoryConfiguration.read(configInputStream, REPO_NAME).with(environment);
        Problems problems = config.validate();
        if (problems.hasProblems()) {
            System.out.println(problems);
            fail("Problems encountered during repository startup: " + problems);
        }
        repository = new JcrRepository(config);
        repository.start();
        session = repository.login();
        rootNode = session.getRootNode();
        addSequencingListeners(session);
    }

    protected void addSequencer( EditableDocument doc,
                                 String desc,
                                 String type,
                                 String... pathExpressions ) {
        EditableDocument sequencing = doc.getOrCreateDocument(FieldName.SEQUENCING);
        EditableDocument sequencers = sequencing.getOrCreateDocument(FieldName.SEQUENCERS);
        // Create the sequencer doc ...
        String name = desc;
        EditableDocument sequencer = Schematic.newDocument();
        sequencer.set(FieldName.NAME, name);
        sequencer.set(FieldName.CLASSNAME, type);
        sequencer.setArray(FieldName.PATH_EXPRESSIONS, (Object[])pathExpressions);
        // Set it on the 'sequencers' doc ...
        sequencers.set(name, sequencer);
    }

    @Test
    public void shouldStartRepositoryWithNoSequencers() throws Exception {
        startRepositoryWithConfiguration("{}");
    }

    @Test
    public void shouldStartRepositoryWithOneSequencer() throws Exception {
        EditableDocument doc = Schematic.newDocument();
        addSequencer(doc, "seq1", TestSequencersHolder.DefaultSequencer.class.getName(), "/foo[@bar] => /output");
        startRepositoryWithConfiguration(doc);

        // Now use a session to add a '/foo' node with a 'bar' property ...
        Node foo = session.getRootNode().addNode("foo");
        foo.setProperty("bar", "value of bar");
        foo.setProperty("baz", "value of baz");
        session.save();

        // Now verify that the test sequencer created a node ...
        Node fooOutput = getOutputNode("/output/foo");
        assertThat(fooOutput, is(notNullValue()));
        Node derivedNode = session.getNode("/output/foo/" + TestSequencersHolder.DERIVED_NODE_NAME);
        assertThat(derivedNode, is(notNullValue()));
    }

    @Test
    public void shouldAllowSequencerToBeConfiguredWithOnlyInputPath() throws Exception {
        EditableDocument doc = Schematic.newDocument();
        addSequencer(doc, "seq1", TestSequencersHolder.DefaultSequencer.class.getName(), "/foo[@bar]");
        startRepositoryWithConfiguration(doc);

        // Now use a session to add a '/foo' node with a 'bar' property ...
        Node foo = session.getRootNode().addNode("foo");
        foo.setProperty("bar", "value of bar");
        foo.setProperty("baz", "value of baz");
        session.save();

        // Now verify that the test sequencer created a node ...
        Node derivedNode = getOutputNode("/foo/" + TestSequencersHolder.DERIVED_NODE_NAME);
        assertNotNull(derivedNode);
        assertThat(derivedNode.getParent(), is(sameInstance(foo)));
    }

    @Test
    public void shouldNotWreakHavocIfSequencerFails() throws Exception {
        EditableDocument doc = Schematic.newDocument();
        addSequencer(doc, "seq1", TestSequencersHolder.FaultyDuringExecute.class.getName(), "/foo[@bar] => /output");
        startRepositoryWithConfiguration(doc);

        // Now use a session to add a '/foo' node with a 'bar' property ...
        Node foo = session.getRootNode().addNode("foo");
        foo.setProperty("bar", "value of bar");
        foo.setProperty("baz", "value of baz");
        session.save();

        expectSequencingFailure(foo);
        // Now verify that there is NO output node, since the sequencer should have failed ...
        assertThat(session.getRootNode().hasNode("output/foo"), is(false));
    }

    /**
     * Sequencer path expressions are matching expressions, and therefore we cannot verify that they actually represent paths. So,
     * even though this is an valid path expression, it won't match any real paths.
     * 
     * @throws Exception
     */
    @Test
    public void shouldCreateStartRepositoryWithValidButUnusableSequencerPathExpression() throws Exception {
        EditableDocument doc = Schematic.newDocument();
        addSequencer(doc, "seq1", TestSequencersHolder.DefaultSequencer.class.getName(), "## valid but unusable");
        startRepositoryWithConfiguration(doc);
    }

    @Test
    public void shouldRemoveSequencerIfItCrashesDuringInitialize() throws Exception {
        EditableDocument doc = Schematic.newDocument();
        addSequencer(doc, "seq1", TestSequencersHolder.FaultyDuringInitialize.class.getName(), "/foo[@bar] => /output");
        startRepositoryWithConfiguration(doc);

        Node foo = session.getRootNode().addNode("foo");
        foo.setProperty("bar", "value of bar");
        session.save();

        Thread.sleep(100L);

        assertEquals(0, TestSequencersHolder.FaultyDuringInitialize.EXECUTE_CALL_COUNTER.get());
    }

    @Test
    public void shouldSupportVariousPropertyTypes() throws Exception {
        startRepositoryWithConfiguration(getClass().getClassLoader()
                                                   .getResourceAsStream("config/repo-config-property-types.json"));
        session.getRootNode().addNode("shouldTriggerSequencer");
        session.save();
        assertNotNull(getOutputNode("/shouldTriggerSequencer/" + TestSequencersHolder.DERIVED_NODE_NAME));
    }

    @Test
    @FixFor( "MODE-1361" )
    public void shouldSequenceWithCriteriaAndRegexInputPath() throws Exception {
        EditableDocument doc = Schematic.newDocument();
        addSequencer(doc, "seq1", TestSequencersHolder.DefaultSequencer.class.getName(), "default://(*.foo)[bar/@baz] => /output");
        startRepositoryWithConfiguration(doc);

        Node foo = session.getRootNode().addNode("foo.foo");
        Node bar = foo.addNode("bar");
        bar.setProperty("baz", "value of baz");
        session.save();

        Node outputNode = getOutputNode("/output/foo.foo");
        assertNotNull(outputNode);
        assertNotNull(outputNode.getNode(TestSequencersHolder.DERIVED_NODE_NAME));
    }

    @Test
    @FixFor( "MODE-1361" )
    public void shouldGenerateCorrectOutputNodePathForCapturingGroup() throws Exception {
        EditableDocument doc = Schematic.newDocument();
        addSequencer(doc,
                     "seq1",
                     TestSequencersHolder.DefaultSequencer.class.getName(),
                     "default://(*.xml)/jcr:content[@jcr:data] => /output/$1");
        startRepositoryWithConfiguration(doc);

        createNodeWithContentFromFile("cars.xml", "cars.xml");

        Node outputNode = getOutputNode("/output/cars.xml");
        assertNotNull(outputNode);
        assertNotNull(outputNode.getNode(TestSequencersHolder.DERIVED_NODE_NAME));
    }
}
